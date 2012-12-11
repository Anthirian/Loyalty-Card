package card;

import javacard.framework.APDU;
import javacard.framework.APDUException;
import javacard.framework.Applet;
import javacard.framework.CardException;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.SystemException;
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

	/* Indices for the authentication status buffer */
	private static final short AUTH_STEP = 0;
	private static final short AUTH_PARTNER = 1;

	/* Buffers in RAM */
	byte[] tmp;
	byte[] authBuf;
	byte[] authState;
	byte[] authPartnerID;

	/** Holds the terminal nonce generated during authentication step four. */
	byte[] NT;
	
	/** Holds the terminal's name received in authentication step four */
	byte[] partnerName;

	/** The applet state (<code>INIT</code> or <code>ISSUED</code>). */
	byte state = CONSTANTS.STATE_INIT;

	/** The cryptograhy object. Handles all encryption and decryption and also stores the current balance of the card */
	Crypto crypto;

	public Card() {
		crypto = new Crypto(this);
		try {
			tmp = JCSystem.makeTransientByteArray(CONSTANTS.APDU_DATA_SIZE_MAX, JCSystem.CLEAR_ON_DESELECT);
			authBuf = JCSystem.makeTransientByteArray(CONSTANTS.DATA_SIZE_MAX, JCSystem.CLEAR_ON_DESELECT); // TODO Ensure correct buffer length
			authState = JCSystem.makeTransientByteArray((short) 2, JCSystem.CLEAR_ON_DESELECT);
			authPartnerID = JCSystem.makeTransientByteArray((short) 1, JCSystem.CLEAR_ON_DESELECT);
			NT = JCSystem.makeTransientByteArray((short) CONSTANTS.NONCE_LENGTH, JCSystem.CLEAR_ON_DESELECT);
			partnerName = JCSystem.makeTransientByteArray(CONSTANTS.NAME_LENGTH, JCSystem.CLEAR_ON_DESELECT);
		} catch (SystemException e) {
			throwException(e.getReason());
		}
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new Card().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) throws ISOException, APDUException {
		// Ignore the CommandAPDU that selects this applet on the card
		if (selectingApplet()) {
			//reset();
			return;
		}
		short responseSize = 0;
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

		short bytesRead = 0;

		// Only use authBuf for authentication, use tmp for everything else
		if (ins == CONSTANTS.INS_AUTHENTICATE) {
			bytesRead = read(apdu, authBuf);
			responseSize = processFurther(authBuf, bytesRead, cla, ins, p1, p2);
		} else {
			bytesRead = read(apdu, tmp);
			responseSize = processFurther(tmp, bytesRead, cla, ins, p1, p2);
		}

		if (responseSize != 0) {
			send(ins, authBuf, responseSize, apdu);
		} else {
			throwException(CONSTANTS.SW1_NO_PRECISE_DIAGNOSIS, CONSTANTS.SW2_INTERNAL_ERROR);
		}
		return;
	}

	/**
	 * Handles the instruction byte with the appropriate buffer. Also sends the ResponseAPDU.
	 * 
	 * @param buf
	 *            the buffer to use for the operation
	 * @param length
	 *            the length of the data in <code>buffer</code>.
	 * @param cla
	 *            the class byte from the APDU.
	 * @param ins
	 *            the instruction byte from the APDU.
	 * @param p1
	 *            the first parameter byte from the APDU.
	 * @param p2
	 *            the second parameter byte from the APDU.
	 * @return the length of the response data.
	 */
	private short processFurther(byte[] buf, short length, byte cla, byte ins, byte p1, byte p2) {
		short responseSize = 0;

		try {
			responseSize = handleInstruction(cla, ins, p1, p2, length, buf);
		} catch (UserException e) {
			throwException(e.getReason());
		}
		
		// Ensure the buffer size is sufficient
		if (responseSize > buf.length) {
			reset();
			throwException(ISO7816.SW_FILE_FULL);
			return 0;
		}
		return responseSize;
	}

	/**
	 * Handles the INStruction byte of an APDU and calls the corresponding functions.
	 * 
	 * @param cla
	 *            the class byte from the APDU.
	 * @param ins
	 *            the instruction byte from the APDU.
	 * @param p1
	 *            the first parameter byte from the APDU.
	 * @param p2
	 *            the second parameter byte from the APDU.
	 * @param length
	 *            the length of the data in <code>buffer</code>.
	 * @param buffer
	 *            the buffer holding the data relevant to the instruction.
	 * @return the size of the data for the ResponseAPDU.
	 */
	private short handleInstruction(byte cla, byte ins, byte p1, byte p2, short length, byte[] buffer) throws UserException {
		short responseSize = 0;

		switch (state) {
		case CONSTANTS.STATE_INIT:
			// When initializing the only supported instruction is the issuance of the card
			switch (ins) {
			case CONSTANTS.INS_ISSUE:
				responseSize = crypto.issueCard();
				break;
			case CONSTANTS.INS_AUTHENTICATE:
				responseSize = authenticate(p1, p2, length, buffer);
				break;
			case CONSTANTS.INS_GET_PUBKEY:
				responseSize = crypto.getPubKeyCard(buffer);
				break;
			default:
				throwException(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		case CONSTANTS.STATE_ISSUED:
			// If the card has been finalized it is ready for regular use
			switch (ins) {
			case CONSTANTS.INS_AUTHENTICATE:
				responseSize = authenticate(p1, p2, length, buffer);
				break;
			case CONSTANTS.INS_BAL_INC:
				responseSize = add(buffer, length);
				break;
			case CONSTANTS.INS_BAL_DEC:
				responseSize = subtract(buffer, length);
				break;
			case CONSTANTS.INS_BAL_CHECK:
				responseSize = checkCredits(buffer);
				break;
			case CONSTANTS.INS_GET_PUBKEY:
				responseSize = crypto.getPubKeyCard(buffer);
				break;
			default:
				throwException(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			break;
		default:
			ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
		}
		return responseSize;
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
	 * Send the data encrypted based on the instruction byte. If we are authenticating we only use RSA, otherwise we use AES.
	 * 
	 * @param type
	 *            the instruction byte
	 * @param data
	 *            the buffer that holds the message to be sent.
	 * @param length
	 *            the length of the message in the buffer.
	 * @param apdu
	 *            the APDU that invoked this response.
	 */
	private void send(short type, byte[] data, short length, APDU apdu) {
		switch (type) {
		case CONSTANTS.INS_AUTHENTICATE:
			sendRSAEncrypted(crypto.getPubKeySupermarket(), data, length, apdu);
			break;
		default:
			
			sendAESEncrypted(data, length, apdu);
			break;
		}
	}

	/**
	 * Encrypts a message with AES and then sends it.
	 * 
	 * @param data
	 *            the buffer that holds the message to be sent.
	 * @param length
	 *            the length of the message in the buffer.
	 * @param apdu
	 *            the APDU that invoked this response.
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
	 * Encrypts a message with RSA and then sends it.
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
		// Check if we are in the correct step
		if (step != authState[AUTH_STEP] + 1) {
			reset();
			UserException.throwIt(CONSTANTS.SW2_AUTH_STEP_INCORRECT);
			return 0;
		}

		try {
			switch (step) {
			case CONSTANTS.P2_AUTHENTICATE_STEP1:
				outLength = authStep2(to, length, buffer);
				break;
			case CONSTANTS.P2_AUTHENTICATE_STEP2:
				outLength = authStep4(to, length, buffer);
				break;
			default:
				throwException(ISO7816.SW_WRONG_P1P2);
			}
		} catch (UserException e) {
			reset();
			UserException.throwIt(e.getReason());
			return 0;
		}

		if (outLength == 0) {
			reset();
			UserException.throwIt((short) CONSTANTS.SW2_AUTH_OTHER_ERROR);
			return 0;
		} else {
			// Everything went fine, so move on to the next step.
			authState[AUTH_STEP] = step;
			authState[AUTH_PARTNER] = to;

			// the last auth step was ok: term has authenticated to card
			if (authState[AUTH_STEP] == CONSTANTS.P2_AUTHENTICATE_STEP2) {
				crypto.enable();
			}

			return outLength;
		}
	}

	/**
	 * Performs the second authentication step. (Steps 1, 3 and 5 are done by the terminal)<br />
	 * 1. T -> C : T<br />
	 * 2. C -> T : {C, T, N_C}pkT<br />
	 * <br />
	 * <b>Note:</b> The <code>buffer</code> is reused to hold the response for the next step.
	 * 
	 * @param to
	 *            the partner to authenticate to.
	 * @param length
	 *            the length of <code>buffer</code>.
	 * @param buffer
	 *            the buffer that holds the first message.
	 * @return the length of the challenge to send back to the terminal, 0 if an exception occurred.
	 * @throws UserException
	 *             if P1 of the CommandAPDU is incorrect
	 */
	private short authStep2(byte to, short length, byte[] buffer) throws UserException {
		short responseSize = 0;
		// if (to != CONSTANTS.P1_AUTHENTICATE_CARD && to != CONSTANTS.P1_AUTHENTICATE_OFFICE) {
		if (to != CONSTANTS.P1_AUTHENTICATE_CARD) { // "to" should be me (the card)
			reset();
			UserException.throwIt(CONSTANTS.SW2_AUTH_WRONG_PARTNER);
			return 0;
		}

		// Check that the message only contains 1 byte as we expect only an identification from the terminal (C -> T : T)
		/*
		if (length != CONSTANTS.NAME_LENGTH) {
			reset();
			throwException(CONSTANTS.SW1_WRONG_LENGTH, CONSTANTS.SW2_AUTH_INCORRECT_MESSAGE_LENGTH);
			return 0;
		}
		*/

		// When my partner != 0, something is wrong so return 0
		if (authState[AUTH_PARTNER] != 0) {
			throwException(CONSTANTS.SW1_WRONG_PARAMETERS, CONSTANTS.SW2_AUTH_WRONG_PARTNER);
			return 0;
		}

		// if the length of the message is correct, assume it to be the name of the terminal
		// terminal has to send its name as defined in the constants
		try {
			authPartnerID[0] = buffer[CONSTANTS.AUTH_MSG_1_OFFSET_NAME_TERM];
		} catch (Exception e) {
			throwException(CONSTANTS.SW1_WRONG_PARAMETERS);
		}

		// flush the buffer to prepare for response
		clear(buffer);
				
		// Add this card's name
		responseSize += crypto.getCardName(buffer, CONSTANTS.AUTH_MSG_2_OFFSET_NAME_CARD);
		throwException((short) 42);
		// Add the previously found partner name
		responseSize += Util.arrayCopyNonAtomic(authPartnerID, (short) 0, buffer, CONSTANTS.AUTH_MSG_2_OFFSET_NAME_TERM, CONSTANTS.NAME_LENGTH);

		// generate a nonce and store it in the buffer
		crypto.generateCardNonce();
		responseSize += crypto.getCardNonce(buffer, CONSTANTS.AUTH_MSG_2_OFFSET_NC);

		// buffer should now hold challenge1 (destined for the terminal): [ C | T | N_C ]
		return responseSize;
	}

	/**
	 * Performs the fourth authentication step (steps 1, 3 and 5 are performed by the terminal):<br />
	 * 1. T -> C : {T, C, N_C, N_T}pkC<br />
	 * 2. C -> T : {C, T, N_T, k}pkT<br />
	 * 
	 * <b>Note</b> The <code>buffer</code> is reused to hold the response data.
	 * 
	 * @param to
	 *            the authentication partner.
	 * @param length
	 *            the length of the ciphertext in the buffer
	 * @param buffer
	 *            the buffer holding the ciphertext of the terminal's challenge.
	 * @return the length of the response data.
	 * @throws UserException
	 *             if P1 of the CommandAPDU is incorrect
	 */
	private short authStep4(byte to, short length, byte[] buffer) throws UserException {
		short responseSize = 0;
		if (authState[AUTH_PARTNER] != to) {
			reset();
			UserException.throwIt((short) CONSTANTS.SW2_AUTH_WRONG_PARTNER);
			return 0;
		}

		if (to != CONSTANTS.P1_AUTHENTICATE_CARD) { // "to" should be me (the card)
			reset();
			Card.throwException(CONSTANTS.SW1_WRONG_PARAMETERS, CONSTANTS.SW2_AUTH_WRONG_PARTNER);
			return 0;
		} // I am the recipient, so continue

		// Decrypt the message
		length = crypto.pubDecrypt(buffer, (short) 0, length, buffer, (short) 0);

		// if the length of the plaintext differs from the predetermined length, assume an error
		if (length != CONSTANTS.AUTH_MSG_3_TOTAL_LENGTH) {
			reset();
			throwException(CONSTANTS.SW1_WRONG_LE_FIELD_00, CONSTANTS.SW2_AUTH_WRONG_2);
			return 0;
		} // The length is correct

		// check for my name in the data, just to make sure
		if (Util.arrayCompare(buffer, CONSTANTS.AUTH_MSG_3_OFFSET_NAME_CARD, CONSTANTS.NAME_CARD, (short) 0, CONSTANTS.NAME_LENGTH) != 0) {
			reset();
			throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_AUTH_WRONG_PARTNER);
			return 0;
		}
		// the data contains my name
		
		// store the name of the terminal locally to ensure sending to the same partner
		if (Util.arrayCopyNonAtomic(buffer, CONSTANTS.AUTH_MSG_3_OFFSET_NAME_TERM, partnerName, (short) 0, CONSTANTS.NAME_LENGTH) == CONSTANTS.NAME_LENGTH) {
			reset();
			throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_AUTH_WRONG_PARTNER);
			return 0;
		}		

		// proceed to check the name of the terminal matches the one we found in step 1

		// This check currently fails, because authState[AUTH_PARTNER] was not initialized in step 1
		// Instead we initialized authPartner[0] with one byte representing the partner
		// if (authState[AUTH_PARTNER] != buffer[CONSTANTS.AUTH_MSG_3_OFFSET_NAME_TERM]) {
		// reset();
		// throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_AUTH_WRONG_PARTNER);
		// return 0;
		// } // the data contains the same terminal name as we found in step 1

		// Check if the challenge has been solved by comparing the received nonce with the one from step 1
		if (!crypto.checkCardNonce(buffer, CONSTANTS.AUTH_MSG_3_OFFSET_NC)) {
			reset();
			throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_AUTH_WRONG_NONCE);
			return 0;
		} // the nonce was verified successfully

		// Store the terminal nonce locally
		Util.arrayCopyNonAtomic(buffer, CONSTANTS.AUTH_MSG_3_OFFSET_NT, NT, (short) 0, CONSTANTS.NONCE_LENGTH);

		// We have checked and received everything needed to build the response, so clear the buffer
		clear(buffer);

		// Prepare the response

		// Add both parties to the response
		try {
			responseSize += Util.arrayCopyNonAtomic(CONSTANTS.NAME_CARD, (short) 0, buffer, CONSTANTS.AUTH_MSG_4_OFFSET_NAME_CARD, CONSTANTS.NAME_LENGTH);
			responseSize += Util.arrayCopyNonAtomic(partnerName, (short) 0, buffer, CONSTANTS.AUTH_MSG_4_OFFSET_NAME_TERM, CONSTANTS.NAME_LENGTH);
		} catch (Exception e) {
			reset();
			throwException(((CardException) e).getReason());
			return 0;
		}

		// Append the Terminal's nonce
		responseSize += Util.arrayCopyNonAtomic(NT, (short) 0, buffer, CONSTANTS.AUTH_MSG_4_OFFSET_NT, CONSTANTS.NONCE_LENGTH);

		// Generate the AES-128 session key and copy it into the buffer
		crypto.generateSessionKey();
		if (crypto.getSessionKey(buffer, CONSTANTS.AUTH_MSG_4_OFFSET_SESSION_KEY) != CONSTANTS.AES_KEY_LENGTH) {
			reset();
			throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, CONSTANTS.SW2_UNSUPPORTED_CRYPTO_MODE);
			return 0;
		} else {
			responseSize += CONSTANTS.AES_KEY_LENGTH;
		}

		// Everything is fine
		return responseSize;
	}

	// private void handshakePseudoCode() {
	/*
	 * CAPITALS represent the message that is being sent. Terminal/Card side indicate the side that has to take action on the message.
	 */
	// --------------------
	// ONE - Terminal side:
	// --------------------
	// 1. Send NAME_TERMINAL
	// T -> C : T
	// ----------------
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

	// --------------------
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

	// ------------------
	// THREE - Card side:
	// ------------------
	// 1. Decrypt with RSA
	// 2. Verify NAME_TERMINAL (from step 1) == parameter 2
	// 3. Verify parameter 3 == N_C (from step 2)
	// 4. Store parameter 4 as terminalNonce
	// -----------------
	// FOUR - Card side:
	// -----------------
	// 5. Generate a new AES-128 Session Key k
	// 6. Concatenate NAME_CARD, NAME_TERMINAL, terminalNonce as well as the AES Session Key m into response2
	// 7. Encrypt response2 with RSA and send:
	// C -> T : {C, T, N_T, k}pkT

	// ---------------------
	// FOUR - Terminal side:
	// ---------------------
	// 1. Decrypt with RSA
	// 2. Verify C and T match previous values
	// 3. Verify N_T matches
	// 4. Store k as the session key to use until the card disconnects
	// }

	/**
	 * Increments the balance of <code>this</code> card by a number of credits.
	 * 
	 * @param buffer
	 *            the buffer holding the amount of (non-zero and non-negative) credits to add. Should <u>only</u> contain the amount. Is overwritten with the
	 *            new balance.
	 * @param length
	 *            the length of the amount in the buffer (must be 2 or an exception will be thrown).
	 * @return length of the new balance in the buffer (2 if successful, 0 otherwise).
	 * @throws UserException
	 *             <ul>
	 *             <li>if the length of the amount in <code>buffer</code> is not 2.</li>
	 *             <li>if the amount of credits to add is <= 0.</li>
	 *             <li>if a balance change operation is already in progress.</li>
	 *             </ul>
	 */
	private short add(byte[] buffer, short length) throws UserException {
		short responseSize = 0;

		// Verify buffer length
		if (length != CONSTANTS.CREDITS_LENGTH) {
			UserException.throwIt((short) CONSTANTS.SW2_CREDITS_WRONG_LENGTH);
			return 0;
		}

		// Find the amount
		short amount = Util.getShort(buffer, (short) 0);

		// Change the balance by amount
		try {
			JCSystem.beginTransaction();
			Util.setShort(buffer, (short) 0, crypto.gain(amount));
			responseSize = 2;
			JCSystem.commitTransaction();
		} catch (ISOException ie) {
			UserException.throwIt((short) CONSTANTS.SW2_CREDITS_NEGATIVE);
			return 0;
		} catch (TransactionException te) {
			UserException.throwIt(CONSTANTS.SW2_INTERNAL_ERROR);
			return 0;
		}

		return responseSize;
	}

	/**
	 * Decrements the balance of <code>this</code> card by a number of credits.
	 * 
	 * @param buffer
	 *            the buffer holding the amount of (non-zero and non-negative) credits to subtract. Should <u>only</u> contain the amount. Is overwritten with
	 *            the new balance.
	 * @param length
	 *            the length of the amount in the buffer (must be 2 or an exception will be thrown).
	 * @return 1 if all went well, or 0 if an exception occurred.
	 * @throws UserException
	 *             <ul>
	 *             <li>if the amount of credits to add is <= 0.</li>
	 *             <li>if the length of the amount in <code>buffer</code> is not 2.</li>
	 *             <li>if a balance change operation is already in progress.</li>
	 *             </ul>
	 */
	private short subtract(byte[] buffer, short length) throws UserException {
		short responseSize = 0;

		// Verify buffer length
		if (length != CONSTANTS.CREDITS_LENGTH) {
			UserException.throwIt((short) CONSTANTS.SW2_CREDITS_WRONG_LENGTH);
			return 0;
		}

		// Find the amount
		short amount = Util.getShort(buffer, (short) 0);

		// Change the balance by amount
		try {
			JCSystem.beginTransaction();
			Util.setShort(buffer, (short) 0, crypto.spend(amount));
			responseSize = 2;
			JCSystem.commitTransaction();
		} catch (ISOException ie) {
			UserException.throwIt((short) CONSTANTS.SW2_CREDITS_NEGATIVE);
			return 0;
		} catch (TransactionException te) {
			UserException.throwIt(CONSTANTS.SW2_INTERNAL_ERROR);
			return 0;
		}

		return responseSize;
	}

	/**
	 * Checks the current balance on the card.
	 * 
	 * @param buffer
	 *            the buffer to hold the amount in. Any data present in the buffer is ignored and overwritten upon retrieval.
	 * @return
	 */
	private short checkCredits(byte[] buffer) {
		short responseSize = 0;
		if (crypto.authenticated()) {
			Util.setShort(buffer, (short) 0, crypto.getBalance());
			responseSize = 2;
		} else {
			throwException(CONSTANTS.SW1_COMMAND_NOT_ALLOWED_00, CONSTANTS.SW2_NO_AUTH_PERFORMED);
			return 0;
		}
		return responseSize;
	}

	/**
	 * Handles the situation where the buffer is full.
	 * 
	 * @param buf
	 *            the array that is full and will be cleared.
	 * @throws ISOException
	 *             when the buffer is full.
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
		clear(authBuf);
		clear(authState);
		clear(authPartnerID);
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

	/**
	 * Handles exceptions by means of status words.
	 * 
	 * @param b1
	 *            the first status word
	 * @param b2
	 *            the second status word
	 */
	public static final void throwException(byte b1, byte b2) {
		throwException(Util.makeShort(b1, b2));
	}

	/**
	 * Handles exceptions by means of status words.
	 * 
	 * @param reason
	 *            the two status words.
	 * @see #throwException(byte, byte)
	 */
	public static final void throwException(short reason) {
		ISOException.throwIt(reason);
	}
}
