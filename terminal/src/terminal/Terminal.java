package terminal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import common.AppletCommunication;
import common.AppletSession;
import common.CONSTANTS;
import common.KeyManager;
import common.TerminalCrypto;

/**
 * The terminal class provides an interface for reading out the loyalty card system
 * @author Geert Smelt
 * @author Robin Oostrum
 *
 */

public class Terminal  {

	AppletCommunication com;
	AppletSession session;
	
	TerminalCrypto crypto;
	RSAPublicKey supermarketPublicKey;
	RSAPrivateKey privKey;
	String keyDir = "./keys/";
	
	int customerId;
	int cardId;
	int sequenceNr;
	
	public Terminal (int customerId) {
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
			ByteBuffer bb = ByteBuffer.wrap(customerSeqBytes);
			sequenceNr = bb.getInt();
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
			byte[] customerSeqBytes = ByteBuffer.allocate(CONSTANTS.SEQNR_BYTESIZE).
					putInt(n).array();
			customerSeqFile.write(customerSeqBytes);
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
		// reset the credits when a new card is inserted
		if (isNewSequence) {
			System.out.println("Credits reset.");
			credits.resetCredits();
		}

		try {
			writeMilage(credits.getCredits());
		} catch (SecurityException e) {
			System.err.println(e.getMessage());
		}

		/* let the user start the engine
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
}