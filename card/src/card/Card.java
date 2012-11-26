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
	
	/** Initialization state. Allows for cryptography initialization */
	private static final byte STATE_INIT = 0;
	/** Issued state. The card is ready for use in a supermarket. */
	private static final byte STATE_ISSUED = 1;

	/** Temporary buffer in RAM. */
	byte[] tmp;

	/** The applet state (<code>INIT</code> or <code>ISSUED</code>). */
	byte state;
	
	/** The cryptograhy object. Handles all encryption and decryption and also stores the current balance of the card */
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

		byte[] buf = apdu.getBuffer();

		byte cla = buf[ISO7816.OFFSET_CLA];
		byte ins = buf[ISO7816.OFFSET_INS];
		byte p1 = buf[ISO7816.OFFSET_P1];
		byte p2 = buf[ISO7816.OFFSET_P2];
		byte lc = buf[ISO7816.OFFSET_LC];

		handleInstruction(apdu, ins);

		byte[] some_buffer_to_store_the_data = { (byte) 0xFF, (byte) 0xFF };
		read(apdu, some_buffer_to_store_the_data);

		// Prepare reponse
		Util.setShort(buf, (short) 1, (short) 0);

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
			case INS_ISSUE:
				// The card is ready for general use.
				state = STATE_INIT;
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

	/**
	 * Reads the APDU data into <code>data</code>. <code>data</code> will be cleared.
	 * 
	 * @param apdu
	 *            the APDU to extract the data field from.
	 * @param data
	 *            target buffer for the data that will be extracted from the APDU's data field. Has to be sufficiently long. 
	 * @return the number of bytes that were read from the APDU.
	 */
	short read(APDU apdu, byte[] data) {
		byte[] buffer = apdu.getBuffer();

		short offset = 0;
		short readCount = apdu.setIncomingAndReceive();
		if (readCount > data.length) {
			memoryFull(data);
			return 0;
		}
		Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, data, offset, readCount);
		offset += readCount;

		while (apdu.getCurrentState() == APDU.STATE_PARTIAL_INCOMING) {
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
			if (offset + readCount > data.length) {
				memoryFull(data);
				return 0;
			}
			Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, data, offset, readCount);
			offset += readCount;
		}
		return offset;
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

	/**
	 * Checks the status of the session, i.e. wheter the handshake has been successfully completed.
	 * 
	 * @return <code>true</code> if <code>this</code> card has an active session.<br />
	 *         <code>false</code> if <code>this</code> has not completed the handshake.
	 */
	boolean isAuthenticated() {
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

		byte[] challenge = {(byte) 0xFF}; // empty

		// Encrypt this challenge
		byte[] ciphertext = crypto.pubEncrypt(challenge);

		// send it
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
		// read(apdu, tmp);
		try {
			crypto.gain((short) 0);
		} catch (ISOException ie) {
			// TODO What to do with the exception once caught?
		}
		return crypto.getBalance();
	}

	private short spend(APDU apdu) {
		// read(apdu, tmp);
		try {
			crypto.spend((short) 0);
		} catch (ISOException ie) {
			// TODO What to do with the exception once caught?
		}
		return crypto.getBalance();
	}

	/**
	 * Check the current balance of the card.
	 * 
	 * @param
	 * @return the current balance.
	 */
	private short check(APDU apdu) {
		// read(apdu, tmp);
		// TODO Make sure the user is authenticated?
		return crypto.getBalance();
	}

	/**
	 * Handles the situation where the buffer is full.
	 * 
	 * @param buf
	 *            the array that is full and will be cleared.
	 * @throws ISOException
	 *             when the <code>buf</code> is full.
	 */
	private void memoryFull(byte[] buf) {
		throwException(ISO7816.SW_FILE_FULL);
		clear(buf);
	}

	/**
	 * Clears the input array, non-atomically writing zeroes from indexes 0 through length - 1.
	 * 
	 * @param buf
	 *            Array to be cleared.
	 */
	private void clear(byte[] buf) {
		Util.arrayFillNonAtomic(buf, (short) 0, (short) buf.length, (byte) 0);
	}

	public static final void throwException(byte b1, byte b2) {
		throwException(Util.makeShort(b1, b2));
	}

	public static final void throwException(short reason) {
		ISOException.throwIt(reason);
	}
}
