package common;

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

/**
 * Class representing a communication channel between the card and a terminal
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class AppletCommunication {

	static final byte[] APPLET_AID = { 0x11, (byte) 0x86, (byte) 0x86, (byte) 0x81, 0x35, 0x24 };

	static final CommandAPDU SELECT_APDU = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, APPLET_AID);

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
			System.err.println("Security exception: " + e.getMessage());
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
			System.out.println(resp);
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
			System.err.println("Communication error: " + e.getMessage());
			throw new SecurityException();
		}
		return rapdu;
	}

	public Response sendCommand(byte instruction, byte p1, byte p2, byte[] data) {
		try {
			Response response = processCommand(instruction, p1, p2, data);
			if (response == null) {
				return null;
			}
			return response;
		} catch (SecurityException e) {
			session.reset();
			messageCounter = 0;
			System.err.println(e.getMessage());
		}
		return null;
	}

	public Response sendCommand(byte instruction, byte[] data) {
		return sendCommand(instruction, (byte) 0, (byte) 0, data);
	}

	public Response sendCommand(byte instruction, byte p1, byte p2) {
		byte[] data = new byte[1];
		data[0] = instruction;
		return sendCommand(instruction, p1, p2, data);
	}

	public Response sendCommand(byte instruction) {
		return sendCommand(instruction, (byte) 0, (byte) 0);
	}

	private Response processCommand(byte instruction, byte p1, byte p2, byte[] data) {
		ResponseAPDU rapdu;

		int bytesToSend = data.length;

		if (bytesToSend > CONSTANTS.DATA_SIZE_MAX) {
			throw new SecurityException();
		}
		
		if (bytesToSend > CONSTANTS.APDU_DATA_SIZE_MAX) {
			throw new SecurityException();
		}

		rapdu = sendSessionCommand(CONSTANTS.CLA_DEF, instruction, p1, p2, data);
		System.out.println("Terminal received: " + rapdu);
		return processResponse(rapdu);
	}

	private ResponseAPDU sendSessionCommand(int cla, int ins, int p1, int p2, byte[] data) {
		if (session.isAuthenticated()) {
			data = crypto.encryptAES(data, session.getSessionKey());
		}

		CommandAPDU apdu = new CommandAPDU(cla, ins, p1, p2, data);
		return sendCommandAPDU(apdu);
	}

	private Response processResponse(ResponseAPDU rapdu) {
		if (rapdu == null) {
			return null;
		}

		Response resp;
		byte[] data = rapdu.getData();

		if (data.length > 0) {
			data = processSessionResponse(data);
			resp = new Response((byte) rapdu.getSW1(), (byte) rapdu.getSW2(), data);
		} else {
			System.out.println("Response-APDU contained no data.");
			resp = new Response((byte) rapdu.getSW1(), (byte) rapdu.getSW2());
		}

		return resp;
	}

	private byte[] processSessionResponse(byte[] data) {
		if (session.isAuthenticated()) {
			data = crypto.decryptAES(data, session.getSessionKey());
		}
		return data;
	}

	void log(CommandAPDU obj) {
		// TODO Actually log something
	}

	void log(Object obj) {
		// TODO Actually log something
	}
}