package card;

import common.CONSTANTS;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;
import javacard.security.Key;

/**
 * Java Card applet to be used for the Loyalty Card system
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class Card extends Applet implements ISO7816 {
	private static final byte INS_ISSUE = (byte) 0x40;

	/* INStructions to save the terminal's public key on the card */
	private static final byte INS_SET_TERM_PUB_MOD = (byte) 0x42;
	private static final byte INS_SET_TERM_PUB_EXP = (byte) 0x52;

	private static final byte INS_MUT_AUTH = (byte) 0xAA;

	/* INStructions that allow the terminal to change or view the amount of credits */
	private static final byte INS_ADD_PTS = (byte) 0xA0;
	private static final byte INS_SPEND_PTS = (byte) 0xB0;
	private static final byte INS_CHECK_BAL = (byte) 0xC0;
	/*
	 * States of the card. If the card is in the initialization state, the crypto keys can be written. If the card has been issued, the keys can be used for
	 * cryptography.
	 */
	private static final byte STATE_INIT = 0;
	private static final byte STATE_ISSUED = 1;

	/** Temporary buffer in RAM. */
	byte[] tmp;

	/** The applet state (INIT or ISSUED). */
	byte state;

	Crypto crypto;

	public Card() {

		// Check if the card has already been initialized. If so, don't do it again
		if (state == STATE_INIT) {
			crypto = new Crypto(this);
			// Set the state of the card to initialization to allow for key generation and uploading
		} else {
			state = STATE_INIT;
		}
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Card().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) {
		// Ignore the CommandAPDU that selects this applet on the card
		if (selectingApplet()) {
			return;
		}

		short le = -1;

		byte[] buf = apdu.getBuffer();

		byte cla = buf[ISO7816.OFFSET_CLA];
		byte ins = buf[ISO7816.OFFSET_INS];
		byte p1 = buf[ISO7816.OFFSET_P1];
		byte p2 = buf[ISO7816.OFFSET_P2];
		byte lc = buf[ISO7816.OFFSET_LC];

		handleInstruction(apdu, ins);

		byte[] some_buffer_to_store_the_data = { (byte) 0xFF, (byte) 0xFF };
		extractData(apdu, some_buffer_to_store_the_data);

		// Prepare reponse
		Util.setShort(buf, (short) 1, (short) 0);
		Util.setShort(buf, (short) 3, value);

		// sendEncrypted();
	}

	/**
	 * Handles the INStruction byte of an APDU and calls the corresponding functions.
	 * 
	 * @param apdu
	 *            the APDU to handle the instruction byte from.
	 * @param ins
	 *            the instruction byte to check
	 */
	private void handleInstruction(APDU apdu, byte ins) {
		switch (state) {
		case STATE_INIT:
			// When initializing we have several options to set cryptographic keys etc.
			switch (ins) {
			case INS_SET_TERM_PUB_MOD:
				// Set the terminal's public key
				break;
			case INS_SET_TERM_PUB_EXP:
				// Set the terminal's public key
				break;
			default:
				throwException(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		case STATE_ISSUED:
			// If the card has been finalized it is ready for regular use
			switch (ins) {
			case INS_MUT_AUTH:
				handshake(apdu);
				break;
			case INS_ADD_PTS:
				add(apdu);
				break;
			case INS_SPEND_PTS:
				spend(apdu);
				break;
			case INS_CHECK_BAL:
				check(apdu);
				break;
			default:
				throwException(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		default:
			ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
		}
	}

	public boolean select() {
		/*
		 * Upon selecting this applet we want the card to authenticate itself to the terminal and vice versa, but this should be done via a CommandAPDU other
		 * than SELECT.
		 */
		return true;
	}

	short extractData(APDU apdu, byte[] data) {
		byte[] buffer = apdu.getBuffer();

		byte cla = buffer[ISO7816.OFFSET_CLA];
		byte ins = buffer[ISO7816.OFFSET_INS];

		return (short) 0;
	}

	/**
	 * Encrypts a message and then sends it.
	 * 
	 * @param data
	 *            the buffer that holds the message to be sent
	 * @param length
	 *            the length of the message in the buffer
	 * @param apdu
	 *            the APDU that invoked this response
	 */
	private void sendEncrypted(byte[] data, short length, APDU apdu) {
		if (isAuthenticated()) {
			length = crypto.symEncrypt(data, (short) 0, length, data, (short) 0);
		}
		if (length > CONSTANTS.APDU_SIZE_MAX || length <= 0) {
			throwException(ISO7816.SW_WRONG_LENGTH);
			return;
		}

		apdu.setOutgoing();
		apdu.setOutgoingLength(length);
		apdu.sendBytesLong(data, (short) 0, length);
		return;
	}

	boolean isAuthenticated() {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Mutually authenticates this applet and the terminal using RSA
	 * 
	 * @param apdu
	 *            The APDU containing the data for the handshake
	 * @return <code>true</code> if <code>this</code> applet was authenticated successfully
	 */
	private void handshake(APDU apdu) {
		// TODO Implement mutual authentication algorithm using RSA

		byte[] challenge;

		// Encrypt this challenge
		byte[] ciphertext = crypto.pubEncrypt(challenge);

		sendEncrypted();
	}

	private byte[] encrypt(byte[] plaintext, Key key) {
		byte[] ciphertext = null;
		return ciphertext;
	}

	private byte[] decrypt(byte[] ciphertext, Key key) {
		byte[] plaintext = null;
		return plaintext;
	}

	private short add(APDU apdu) {
		// TODO Add credits to the current balance
		extractData(apdu);
		try {
			crypto.gain((short) 0);
		} catch (NegativeArgumentException e) {

		}
		return balance;
	}

	private short spend(APDU apdu) {
		// TODO Subtract credits from the current balance, if sufficient
		extractData(apdu);
		short price = 0;
		balance = (short) (balance - price);
		return balance;
	}

	private short check(APDU apdu) {
		// TODO Report the current balance of the card
		extractData(apdu);
		return balance;
	}

	public static final void throwException(byte b1, byte b2) {
		throwException(Util.makeShort(b1, b2));
	}

	public static final void throwException(short reason) {
		ISOException.throwIt(reason);
	}
}
