package card;

import common.CONSTANTS;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.TransactionException;
import javacard.framework.UserException;
import javacard.framework.Util;
import javacard.security.Key;

/**
 * Java Card applet to be used for the Loyalty Card system
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class Card extends Applet implements ISO7816 {

	/** Buffer in RAM, which's size is the length of an APDU data field */
	byte[] tmp;

	/** The applet state (<code>INIT</code> or <code>ISSUED</code>). */
	byte state;

	/** The cryptograhy object. Handles all encryption and decryption and also stores the current balance of the card */
	Crypto crypto;

	public Card() {
		// Check if the card has already been initialized. If so, don't do it again
		if (state == CONSTANTS.STATE_INIT) {
			crypto = new Crypto(this);
			tmp = JCSystem.makeTransientByteArray(CONSTANTS.APDU_DATA_SIZE_MAX, JCSystem.CLEAR_ON_DESELECT);
			// Set the state of the card to initialization to allow for key generation and uploading
		} else {
			state = CONSTANTS.STATE_INIT;
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
		short lc = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);

		if (lc > CONSTANTS.APDU_SIZE_MAX || lc == 0) {
			reset();
			throwException(CONSTANTS.SW1_WRONG_LE_FIELD_00, CONSTANTS.SW2_LC_INCORRECT);
			return;
		}

		try {
			handleInstruction(apdu, ins);
		} catch (UserException e) {
			// TODO Auto-generated catch block
		}

		short numberOfBytes = read(apdu, tmp);

		// Prepare reponse
		Util.setShort(buf, ISO7816.OFFSET_CLA, (short) 0);
		Util.setShort(buf, ISO7816.OFFSET_INS, (short) 0);
		Util.setShort(buf, ISO7816.OFFSET_CDATA, (short) 0);

		// sendEncrypted();
	}

	/**
	 * Handles the INStruction byte of an APDU and calls the corresponding functions.
	 * 
	 * @param apdu
	 *            the APDU to handle the instruction byte from.
	 * @param ins
	 *            the instruction byte to check
	 * @throws UserException
	 *             when the length of the amount of credits is longer than two bytes, i.e. not a short.
	 */
	private void handleInstruction(APDU apdu, byte ins) throws UserException {
		switch (state) {
		case CONSTANTS.STATE_INIT:
			// When initializing the only supported instruction is the issuance of the card
			switch (ins) {
			case CONSTANTS.INS_ISSUE:
				state = CONSTANTS.STATE_INIT;
				break;
			default:
				throwException(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		case CONSTANTS.STATE_ISSUED:
			// If the card has been finalized it is ready for regular use
			switch (ins) {
			case CONSTANTS.INS_AUTHENTICATE:
				handshakeStepOne(apdu);
				break;
			case CONSTANTS.INS_BAL_INC:
				if (read(apdu, tmp) == 2) {
					add(Util.makeShort(tmp[0], tmp[1]));
				} else {
					UserException.throwIt(CONSTANTS.SW2_CREDITS_WRONG_LENGTH);
				}
				break;
			case CONSTANTS.INS_BAL_DEC:
				if (read(apdu, tmp) == 2) {
					this.spend(Util.makeShort(tmp[0], tmp[1]));
				} else {
					UserException.throwIt(CONSTANTS.SW2_CREDITS_WRONG_LENGTH);
				}
				break;
			case CONSTANTS.INS_BAL_CHECK:
				checkCredits();
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
	private void sendAESEncrypted(byte[] data, short length, APDU apdu) {
		if (crypto.authenticated()) {
			length = crypto.symEncrypt(data, (short) 0, length, data, (short) 0);
		}
		if (length > CONSTANTS.APDU_DATA_SIZE_MAX || length <= 0) {
			throwException(ISO7816.SW_WRONG_LENGTH);
			return;
		}

		apdu.setOutgoing();
		apdu.setOutgoingLength(length);
		apdu.sendBytesLong(data, (short) 0, length);
		return;
	}

	/**
	 * Send a message encrypted with RSA 512.
	 * 
	 * @param key
	 *            the receiving party's public key.
	 * @param data
	 *            the buffer that holds the message to be sent.
	 * @param length
	 *            the length of the message in the buffer.
	 * @param apdu
	 *            the APDU that invoked this response.
	 */
	private void sendRSAEncrypted(Key key, byte[] data, short length, APDU apdu) {
		if (!crypto.authenticated()) {
			length = crypto.pubEncrypt(key, data, (short) 0, length, data, (short) 0);
		} else {
			throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_AUTH_ALREADY_PERFORMED);
		}

		if (length > CONSTANTS.APDU_DATA_SIZE_MAX || length <= 0) {
			throwException(ISO7816.SW_WRONG_LENGTH);
			return;
		}

		apdu.setOutgoing();
		apdu.setOutgoingLength(length);
		apdu.sendBytesLong(data, (short) 0, length);
	}

	short authenticate(byte to, byte step, short length, byte[] buffer) throws UserException {
		short outLength = 0;

		if (step != authState[AUTHSTATE_STEP] + 1) {
			resetSession(buffer);
			UserException.throwIt(CONSTANTS.SW2_AUTH_STEP_INCORRECT);
			return 0;
		}

		try {
			if (step == CONSTANTS.P2_AUTHENTICATE_STEP1) {
				outLength = authStep1(to, length, buffer);
			} else if (step == CONSTANTS.P2_AUTHENTICATE_STEP2) {
				outLength = authStep2(to, length, buffer);
			}
		} catch (UserException e) {
			resetSession(buffer);
			UserException.throwIt(e.getReason());
			return 0;
		}

		if (outLength == 0) {
			resetSession(buffer);
			UserException.throwIt((short) CONSTANTS.SW2_AUTH_OTHER_ERROR);
			return 0;
		} else {
			authState[AUTHSTATE_STEP] = step;
			authState[AUTHSTATE_PARTNER] = to;
			return outLength;
		}
	}

	private short authStep1(byte to, short length, byte[] buffer) throws UserException {
		short encLength = 0;

		if (to != CONSTANTS.P1_AUTHENTICATE_CARD && to != CONSTANTS.P1_AUTHENTICATE_OFFICE) {
			reset();
			Card.throwException(CONSTANTS.SW1_WRONG_PARAMETERS, CONSTANTS.SW2_AUTH_WRONG_PARTNER);
			return (short) 0;
		}

		/* Always generate nonce. */
		crypto.generateCardNonce();

		crypto.getCardNonce(buffer, CONSTANTS.AUTH_MSG_1_OFFSET_NA);
		crypto.getPubKeyCard(buffer, CONSTANTS.AUTH_MSG_1_OFFSET_SIGNED_PUBKEY);
		
		encLength = crypto.pubEncrypt(to, buffer, (short) 0, CONSTANTS.AUTH_MSG_1_LENGTH, buffer, (short) 0);

		if (authState[AUTHSTATE_PARTNER] != 0) {
			encLength = 0;
		}

		return encLength;
	}

	private short authStep2(byte to, short length, byte[] buffer) throws UserException {
		length = crypto.pubDecrypt(buffer, (short) 0, length, buffer, (short) 0);

		if (length != CONSTANTS.AUTH_MSG_2_LENGTH) {
			reset();
			Card.throwException(CONSTANTS.SW1_WRONG_LE_FIELD_00, CONSTANTS.SW2_AUTH_WRONG_2);
			return 0;
		}

		Util.arrayCopyNonAtomic(buffer, CONSTANTS.AUTH_MSG_2_OFFSET_NB, crypto.getTermNonce(), (short) 0, CONSTANTS.NONCE_LENGTH);

		if (!crypto.checkCardNonce(buffer, CONSTANTS.AUTH_MSG_2_OFFSET_NA)) {
			reset();
			UserException.throwIt((short) CONSTANTS.SW2_AUTH_WRONG_NONCE);
			return 0;
		}

		if (authState[AUTHSTATE_PARTNER] != to) {
			reset();
			UserException.throwIt((short) CONSTANTS.SW2_AUTH_WRONG_PARTNER);
			return 0;
		}

		if (to == CONSTANTS.P1_AUTHENTICATE_CAR) {
			if (!crypto.checkCarID(buffer, CONSTANTS.AUTH_MSG_2_OFFSET_ID)) {
				reset();
				UserException.throwIt((short) CONSTANTS.SW2_AUTH_WRONG_CARID);
				return 0;
			}
		}

		// Clear buffer before reuse.
		clear(buffer);

		// Build response.
		Util.arrayCopyNonAtomic(crypto.getTermNonce(), (short) 0, buffer, CONSTANTS.AUTH_MSG_3_OFFSET_NB, CONSTANTS.NONCE_LENGTH);
		crypto.getCert(buffer, CONSTANTS.AUTH_MSG_3_OFFSET_CERT);
		// Util.arrayCopyNonAtomic(crypto.getCert(), (short) 0, buffer,
		// CONSTANTS.AUTH_MSG_3_OFFSET_CERT, CONSTANTS.CERT_LENGTH);

		length = crypto.pubEncrypt(to, buffer, (short) 0, CONSTANTS.AUTH_MSG_3_LENGTH, buffer, (short) 0);

		if (length == 0) {
			UserException.throwIt((short) CONSTANTS.SW2_AUTH_OTHER_ERROR);
			return 0;
		}

		return length;
	}

	/**
	 * Note that we read the APDU here ourselves instead of in the process function
	 * 
	 * @param apdu
	 */
	private void handshakeStepOne(APDU apdu) {
		// TODO Implement mutual authentication algorithm using RSA.

		// Use P1 and P2 to determine which step is active.

		// ONE - Terminal side:
		// --------------------
		// 1. Send NAME_TERMINAL
		// T -> C : T

		// ONE - Card side:
		// ----------------
		// 1. decrypt with RSA
		// 2. verify data field length is 1 and assume it contains NAME_TERMINAL
		// ----------------
		// TWO - Card side:
		// ----------------
		// 3. Generate nonce N_C
		// 4. Concatenate CONSTANTS.NAME_CARD, CONSTANTS.NAME_TERMINAL and N_C into challenge1
		// 5. Encrypt challenge1 with RSA and send
		// C -> T : {C, T, N_C}pkT
		byte[] nonce = null;
		crypto.generateNonce(nonce);
		byte from = (byte) 0xCC; // C for Card.
		byte to = (byte) 0xAF; // Randomly chosen
		byte[] challenge = null;
		sendRSAEncrypted(crypto.getCompanyKey(), challenge, (short) challenge.length, apdu);

		// TWO - Terminal side:
		// --------------------
		// 1. Decrypt with RSA
		// 2. Retrieve decrypted_challenge[0] and store
		// 3. Verify decrypted_challenge[1] == CONSTANTS.NAME_TERMINAL
		// 4. Assume byte[] cardNonce = decrypted_challenge[1-len]
		// ----------------------
		// THREE - Terminal side:
		// ----------------------
		// 5. Generate nonce N_T
		// 6. Concatenate CONSTANTS.NAME_TERMINAL, CONSTANTS.NAME_CARD, N_C and N_T into response1challenge2
		// 7. Encrypt response1challenge2 with RSA and send:
		// T -> C : {T, C, N_C, N_T}pkC

		// THREE - Card side:
		// ------------------
		// 1. Decrypt with RSA
		// 2. Verify NAME_TERMINAL (from step 1) == parameter 2
		// 3. Verify parameter 3 == N_C (from step 2)
		// 4. Store parameter 4 as terminalNonce
		// -----------------
		// FOUR - Card side:
		// -----------------
		// 5. Concatenate NAME_CARD, NAME_TERMINAL and terminalNonce into response2
		// 6. Encrypt response2 with RSA and send:
		// C -> T : {C, T, N_T}pkT
	}

	/**
	 * Adds an amount of credits to the balance of <code>this</code> card.
	 * 
	 * @param amount
	 *            the amount of (non-zero and non-negative) credits to add to the balance.
	 * @return the new balance.
	 */
	private short add(short amount) {
		try {
			JCSystem.beginTransaction();
			crypto.gain(amount);
			JCSystem.commitTransaction();
		} catch (ISOException ie) {
			// TODO What to do with the exception once caught?
		} catch (TransactionException te) {

		}
		return crypto.getBalance();
	}

	/**
	 * Decrements the balance of <code>this</code> card by a number of credits.
	 * 
	 * @param amount
	 *            the (non-negative) amount of credits to subtract.
	 * @return the new balance.
	 */
	private short spend(short amount) {
		try {
			JCSystem.beginTransaction();
			crypto.spend(amount);
			JCSystem.commitTransaction();
		} catch (ISOException ie) {
			// TODO What to do with the exception once caught?
		} catch (TransactionException te) {

		}
		return crypto.getBalance();
	}

	/**
	 * Check the current balance of the card.
	 * 
	 * @return the current balance.
	 */
	private short checkCredits() {
		if (crypto.authenticated()) {
			return crypto.getBalance();
		} else {
			throwException(CONSTANTS.SW1_COMMAND_NOT_ALLOWED_00, CONSTANTS.SW2_NO_AUTH_PERFORMED);
			return 0;
		}
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
	 * Resets all buffers and crypto-related objects
	 */
	void reset() {
		JCSystem.beginTransaction();
		clear(tmp);
		crypto.clearSessionData();
		JCSystem.commitTransaction();
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
