package supermarketterminal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Date;

import common.AppletCommunication;
import common.AppletSession;
import common.CONSTANTS;
import common.Formatter;
import common.KeyManager;
import common.Response;
import common.TerminalCrypto;

/**
 * The terminal class provides an interface for reading out the loyalty card system
 * @author Geert Smelt
 * @author Robin Oostrum
 *
 */

public class SupermarketTerminal  {

	AppletCommunication com;
	AppletSession session;
	
	TerminalCrypto crypto;
	RSAPublicKey supermarketPublicKey;
	RSAPrivateKey privKey;
	String keyDir = "./keys/";
	
	int customerId;
	int cardId;
	int sequenceNr;
	
	public SupermarketTerminal (int customerId) {
		this.customerId = customerId;
		System.out.println("Welcome customer no. " + customerId);
		loadKeyFiles();
		loadSequenceNumber();
	}
	
	private void loadKeyFiles() {
		try {
			String car = "Customer_" + this.customerId;
			privKey = (RSAPrivateKey) KeyManager.loadKeyPair(car).getPrivate();
			supermarketPublicKey = (RSAPublicKey) KeyManager.loadKeyPair("supermarket")
					.getPublic();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSequenceNumber() {
		try {
			// load sequence number
			FileInputStream carSeqFile = new FileInputStream("customerseq_"
					+ customerId);
			byte[] customerSeqBytes = new byte[CONSTANTS.SEQNR_BYTESIZE];
			carSeqFile.read(customerSeqBytes, 0, 4);
			sequenceNr = Formatter.byteArrayToInt(customerSeqBytes);
			carSeqFile.close();
		} catch (FileNotFoundException e) {
			// generate new seqnum file
			writeCustomerSequenceNr(1);
			sequenceNr = 1;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeCustomerSequenceNr(int n) {
		try {
			FileOutputStream customerSeqFile = new FileOutputStream("customerseq_"
					+ customerId);
			customerSeqFile.write(Formatter.toByteArray(n));
			customerSeqFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void main() {
		// wait until a card is inserted
		com.waitForCard();
		// authenticate the card and the terminal
		System.out.println("Authenticating card...");
		if (!session.authenticate(CONSTANTS.P1_AUTHENTICATE_CAR)) {
			System.err.println("Authentication error.");
			return;
		}

		cardId = session.getCardId();
		byte[] certificate = session.getCertificate();
		
		boolean isNewSequence;
		try {
			isNewSequence = validateCertificate(certificate);
		} catch (SecurityException e) {
			System.err.println(e.getMessage());
			return;
		}
		
		System.out.println("Card authenticated.");
		
		/*
		// reset the credits when a new card is inserted
		if (isNewSequence) {
			System.out.println("Credits reset.");
			credits.resetCredits();
		}

		try {
			writeCredits(credits.getCredits());
		} catch (SecurityException e) {
			System.err.println(e.getMessage());
		}

		// let the user start the engine
		System.out.print("Press return to start the engine.");
		waitForInput();
		try {
			setCardTearFlag();
		} catch (SecurityException e) {
			CLI.showln(e.getMessage());
			return;
		}
		if (motor.isPaused()) {
			motor.restart();
		} else {
			motor.start();
		}
		System.out
				.println("Engine started, press return to stop the engine again.");
		// let the user stop the engine
		waitForInput();
		motor.shutdown();
		sleep(2);
		System.out.println("Engine stopped.");
		// write the mileage data
		try {
			writeMilage(motor.getMileage());
		} catch (SecurityException e) {
			System.err.println(e.getMessage());
		}
		*/
	}
	
	/*
	private boolean validateCertificate(byte[] certificate) {
		byte[] certificateData;

		try {
			certificateData = crypto.verify(certificate, supermarketPublicKey);
		} catch (SignatureException e) {
			throw new SecurityException("Invalid certificate signature");
		}

		// extract the data
		byte[] cardIdBytes = Arrays.copyOfRange(certificateData,
				CONSTANTS.CERT_OFFSET_CARDID, CONSTANTS.CERT_OFFSET_CARDID
						+ CONSTANTS.ID_LENGTH);
		int receivedCardId = Formatter.byteArrayToInt(cardIdBytes);

		byte[] customerIdBytes = Arrays.copyOfRange(certificateData,
				CONSTANTS.CERT_OFFSET_CARID, CONSTANTS.CERT_OFFSET_CARID
						+ CONSTANTS.ID_LENGTH);
		int receivedCustomerId = Formatter.byteArrayToInt(customerIdBytes);

		byte[] sequenceNumberBytes = Arrays.copyOfRange(certificateData,
				CONSTANTS.CERT_OFFSET_SEQNUM, CONSTANTS.CERT_OFFSET_SEQNUM
						+ CONSTANTS.SEQ_LENGTH);
		int receivedSequenceNumber = Formatter
				.byteArrayToInt(sequenceNumberBytes);

		byte[] startDateBytes = Arrays.copyOfRange(certificateData,
				CONSTANTS.SEQ_LENGTH, CONSTANTS.SEQ_LENGTH
						+ CONSTANTS.DATE_LENGTH);
		long startDate = Formatter.byteArrayToLong(startDateBytes);
		byte[] endDateBytes = Arrays.copyOfRange(certificateData,
				CONSTANTS.CERT_OFFSET_END, CONSTANTS.CERT_OFFSET_END
						+ CONSTANTS.DATE_LENGTH);
		long endDate = Formatter.byteArrayToLong(endDateBytes);

		// check certificate date
		Date date = new Date();
		long currentDate = date.getTime() / 1000;
		if (currentDate > endDate || currentDate < startDate) {
			throw new SecurityException("Invalid certificate date (current: "
					+ currentDate + ", start: " + startDate + ", end: "
					+ endDate + ")");
		}

		// check the id's
		if (receivedCardId != this.cardId) {
			throw new SecurityException("Invalid card id in certificate: "
					+ receivedCardId);
		}
		if (receivedCustomerId != this.customerId) {
			throw new SecurityException("Invalid customer id in certificate: "
					+ receivedCustomerId);
		}

		// check the sequence number
		if (this.sequenceNr == receivedSequenceNumber) {
			System.out.println("Sequence number: " + this.sequenceNr);
			return false;
		} else if ((this.sequenceNr + 1) == receivedSequenceNumber) {
			this.sequenceNr = receivedSequenceNumber;
			writeCustomerSequenceNr(receivedSequenceNumber);
			System.out.println("Selected next sequence number: "
					+ this.sequenceNr);
			return true;
		} else {
			throw new SecurityException(
					"Invalid sequence number in certificate: "
							+ receivedSequenceNumber + " current customer sequence number: "
							+ this.sequenceNr);
		}
	}
	*/
	/*
	private void setCardTearFlag() {
		if (!session.isAuthenticated()) {
			throw new SecurityException(
					"Cannot set tear flag, card not authenticated.");
		}
		Response resp = com.sendCommand(CONSTANTS.INS_SET_TEAR);
		if (resp == null) {
			throw new SecurityException("Cannot set tear, card removed.");
		}
		if (!resp.success()) {
			throw new SecurityException("Error writing tear.");
		}
		System.out.println("Card tear flag set.");
	}
	*/
	
	private void writeCredits(int credits) {
		if (!session.isAuthenticated()) {
			throw new SecurityException(
					"Cannot write credits, card not authenticated.");
		}
		byte[] creditsData = Formatter.toByteArray(credits);
		creditsData = crypto.sign(creditsData, privKey);
		Response resp = com.sendCommand(CONSTANTS.INS_WRITE_CREDITS,
				creditsData);
		if (resp == null) {
			throw new SecurityException("Cannot write credits, card removed.");
		}
		if (!resp.success()) {
			throw new SecurityException("Error writing credits.");
		}
		System.out.println("Credits written to card: " + credits);
	}
	
	private void waitForInput() {
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void waitToTryAgain() {
		sleep(1);
		System.out.print("Try to authenticate card again in ");
		for (int i = 5; i > 0; i--) {
			System.out.print(i + "... ");
			sleep(1);
		}
		System.out.println();
	}
	
	private void sleep(int x) {
		try {
			Thread.sleep(1000 * x);
		} catch (InterruptedException e) {
			System.err.println("Session was interrupted");
		}
	}
	
	public static void main(String[] arg) {
		// Change integer to x to represent another terminal with CustomerID x.
		new SupermarketTerminal(1);
	}
}