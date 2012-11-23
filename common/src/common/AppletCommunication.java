package common;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;


public class AppletCommunication {

	static final byte[] APPLET_AID = { 0xB, 0x56, 0x56, 0x51, 0x23, 0x18 };

	static final CommandAPDU SELECT_APDU = new CommandAPDU(
    		(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, APPLET_AID);
	
	private Card card;
	private CardChannel applet;
	private TerminalCrypto crypto;
	private AppletSession session;

	private byte messageCounter;
	
	public AppletCommunication(AppletSession session) {
		this.session = session;
		this.session.setAppletCommunication(this);
		this.crypto = new TerminalCrypto();
	}
	
	public void waitForCard() {
		System.out.print("Waiting for card...");
		while (!connect()) {
			sleep(1);
		}
		System.out.println();
		System.out.println("Card found: " + applet.getCard());
	}
	
	public boolean connect() {
		try {
			if (connectToCard()) {
				if (selectApplet()) {
					this.session.reset();
					this.messageCounter = 0;
					return true;
				}
			}
		} catch (SecurityException e) {
			System.err.println();
			System.err.println(e.getMessage());
			sleep(1);
			System.out.print("Waiting for card...");
		}
		return false;
	}
	
	private boolean requireCard() {
		if (connectToCard()) {
			return true;
		}
		return false;
	}
	
	private boolean connectToCard() {
		TerminalFactory tf = TerminalFactory.getDefault();
		CardTerminals ct = tf.terminals();
		try {
			List<CardTerminal> cs = ct.list(CardTerminals.State.CARD_PRESENT);
			if (cs.isEmpty()) {
				return false;
			}
			CardTerminal t = cs.get(0);
			if (t.isCardPresent()) {
				card = t.connect("*");
				applet = card.getBasicChannel();
				return true;
			}
			return false;
		} catch (CardException e) {
			return false;
		}
	}
	
	private boolean selectApplet() {
		ResponseAPDU resp;
		try {
			resp = applet.transmit(SELECT_APDU);
		} catch (CardException e) {
			return false;
		}
		if (resp.getSW() != 0x9000) {
			throw new SecurityException();
		}
		return true;
	}
	
	private void sleep(int x) {
		try {
			Thread.sleep(1000 * x);
		} catch (InterruptedException e) {
			// This is not a SIGINT, but Thread.interrupt() which we only use on
			// the VehicleMotor.
			System.err.println("Terminal interrupted.");
		}
	}
	
	public ResponseAPDU sendCommandAPDU(CommandAPDU capdu) {
		ResponseAPDU rapdu;
		log(capdu);
		try {
			if (requireCard()) {
				rapdu = applet.transmit(capdu);
			} else {
				return null;
			}
			log(rapdu);
		} catch (CardException e) {
			throw new SecurityException("Communication error: "
					+ e.getMessage());
		}
		return rapdu;
	}
	
	public Response sendCommand(byte instruction, byte p1, byte p2, byte[] data) {
		try {
			// Always add instruction byte.
			if (data != null && data.length > 0) {
				data = Arrays.copyOf(data, data.length + 1);
				data[data.length - 1] = instruction;
			} else {
				data = new byte[] { instruction };
			}

			// sign data + instruction byte when authenticated
			if (session.isAuthenticated()) {
				data = crypto.sign(data, session.getPrivateKey());
			}
			// send command
			Response response = processCommand(instruction, p1, p2, data);
			if (response == null) {
				return null;
			}
			verifyResponse(response, instruction);
			return response;
		} catch (SecurityException e) {
			session.reset();
			messageCounter = 0;
			// System.err.println(e.getMessage());
		}
		return null;
	}
	
	private Response verifyResponse(Response response, byte instruction)
			throws SecurityException {
		// verify signature
		byte[] responseData;
		responseData = response.getData();
		if (responseData == null) {
			throw new SecurityException();
		}
		
		if (session.isAuthenticated()) {
			if (responseData.length < CONSTANTS.RSA_SIGNATURE_LENGTH + 1) {
				throw new SecurityException();
			} else {
				try {
					responseData = crypto.verify(responseData, session
							.getCardPublicKey());
				} catch (SignatureException e) {
					throw new SecurityException();
				}
			}
		}
		
		// instruction byte
		if (responseData[responseData.length - 1] != instruction) {
			throw new SecurityException();
		}
		response.setData(Arrays.copyOfRange(responseData, 0,
				responseData.length - 1));
		return response;
	}
	
	public Response sendCommand(byte instruction, byte[] data) {
		return sendCommand(instruction, (byte) 0, (byte) 0, data);
	}
	
	public Response sendCommand(byte instruction, byte p1, byte p2) {
		byte[] data = new byte[0];
		return sendCommand(instruction, p1, p2, data);
	}
	
	public Response sendCommand(byte instruction) {
		return sendCommand(instruction, (byte) 0, (byte) 0);
	}
	
	private Response processCommand(byte instruction, byte p1, byte p2,
			byte[] data) {
		ResponseAPDU rapdu;
		Response resp;

		int bytesToSend = data.length;
		int bytesSent = 0;
		if (bytesToSend > CONSTANTS.DATA_SIZE_MAX) {
			throw new SecurityException();
		}
		while (bytesToSend > CONSTANTS.APDU_DATA_SIZE_MAX) {
			rapdu = sendSessionCommand(CONSTANTS.CLA_CHAIN_FIRST_OR_NEXT
					| CONSTANTS.CLA_DEF, instruction, p1, p2, Arrays
					.copyOfRange(data, bytesSent, bytesSent
							+ CONSTANTS.APDU_DATA_SIZE_MAX));
			bytesToSend -= CONSTANTS.APDU_DATA_SIZE_MAX;
			bytesSent += CONSTANTS.APDU_DATA_SIZE_MAX;
			resp = processResponse(rapdu);
			if (resp == null) {
				return null;
			}
			if (!resp.success()) {
				throw new SecurityException();
			}
		}
		rapdu = sendSessionCommand(CONSTANTS.CLA_CHAIN_LAST_OR_NONE
				| CONSTANTS.CLA_DEF, instruction, p1, p2, Arrays.copyOfRange(
				data, bytesSent, bytesSent + bytesToSend));
		return processResponse(rapdu);
	}
	
	private ResponseAPDU sendSessionCommand(int cla, int ins, int p1, int p2,
			byte[] data) {
		if (data == null || data.length == 0) {
			throw new SecurityException();
		}
		byte[] msg = new byte[data.length + 1];
		// prepend counter byte
		msg[0] = messageCounter;
		// increment message counter
		messageCounter++;
		if ((messageCounter & 0xff) >= 254) {
			throw new SecurityException();
		}
		System.arraycopy(data, 0, msg, 1, data.length);

		if (session.isAuthenticated()) {
			msg = crypto.encryptAES(msg, session.getSessionKey());
		}

		CommandAPDU apdu = new CommandAPDU(cla, ins, p1, p2, msg);
		return sendCommandAPDU(apdu);
	}
	
	private Response processResponse(ResponseAPDU rapdu) {
		if (rapdu == null) {
			return null;
		}

		Response resp;

		// Prepare response array with single instruction byte for MORE_DATA
		// commands.
		byte[] ins = new byte[] { CONSTANTS.INS_GET_RESPONSE };
		// Add signature if the session is authenticated.
		if (session.isAuthenticated()) {
			ins = crypto.sign(ins, session.getPrivateKey());
		}

		// Retrieve data from first Response-APDU.
		byte[] data = rapdu.getData();
		if (data.length > 0) {
			data = processSessionResponse(data);
		} else {
			// System.out.println("Response-APDU contained no data.");
		}

		// Process Response-APDUs indicating more data available at card.
		while (rapdu.getSW1() == CONSTANTS.SW1_MORE_DATA
				&& rapdu.getSW2() == CONSTANTS.SW2_MORE_DATA) {
			// Send retrieval command
			rapdu = sendSessionCommand(CONSTANTS.CLA_CHAIN_LAST_OR_NONE
					| CONSTANTS.CLA_DEF, CONSTANTS.INS_GET_RESPONSE, 0, 0,
					ins);

			// Add data from Response-APDU to already retrieved data.
			byte[] rdata = rapdu.getData();
			if (rdata.length > 0) {
				rdata = processSessionResponse(rdata);
			} else {
				// System.out.println("*RESPONSE-CHAINED* Response-APDU contained no data.");
			}
			int dataLength = data.length;
			data = Arrays.copyOf(data, dataLength + rdata.length);
			System.arraycopy(rdata, 0, data, dataLength, rdata.length);
		}

		if (data.length > 0) {
			resp = new Response((byte) rapdu.getSW1(), (byte) rapdu.getSW2(),
					data);
		} else {
			resp = new Response((byte) rapdu.getSW1(), (byte) rapdu.getSW2());
		}
		return resp;
	}
	
	private byte[] processSessionResponse(byte[] data) {
		// Decrypt response
		if (session.isAuthenticated()) {
			data = crypto.decryptAES(data, session.getSessionKey());
		}
		// Check and increment messagecounter
		if (messageCounter != data[0]) {
			throw new SecurityException();
		} else {
			messageCounter++;
			// Strip the message counter from response.
			data = Arrays.copyOfRange(data, 1, data.length);
		}
		return data;
	}
	
	void log(CommandAPDU obj) {
		//
	}
	
	void log(Object obj) {
		//
	}
}