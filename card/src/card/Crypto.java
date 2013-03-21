package card;

import javacard.framework.CardRuntimeException;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.CryptoException;
import javacard.security.Key;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.RSAPrivateCrtKey;
import javacard.security.RSAPublicKey;
import javacard.security.RandomData; //import javacard.security.Signature;
import javacardx.crypto.Cipher;


/**
 * The cryptography class handles all cryptographic operations, including but not limited to key generation, nonce generation, encryption and decryption.
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public final class Crypto {

	private Cipher rsaCipher;
	private Cipher aesCipher;

	private RandomData random;
	private byte[] cardNonce;
	private byte[] tmpKey;
	private AESKey sessionKey;

	// private byte[] pubKeyCard;
	private RSAPrivateCrtKey privKeyCard;

	/** The public key of the supermarket */
	private RSAPublicKey pubKeySupermarket;
	private RSAPublicKey pubKeyCard;

	/** The state of authentication of this card, an array of size one */
	private byte[] authState;

	/** The balance of the loyalty card. A <code>short</code> because we assume a maximum of 25000 pts, which is < 2^15 - 1 */
	private short balance;

	/** The applet as uploaded onto the card */
	private Card c;

	/**
	 * Handles key generation and key storage.
	 * 
	 * @param card
	 *            The card to link the cryptographic functions to.
	 */
	public Crypto(Card card) {
		
		pubKeyCard = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, 
				KeyBuilder.LENGTH_RSA_512, false); 
        privKeyCard = (RSAPrivateCrtKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_CRT_PRIVATE, 
        		KeyBuilder.LENGTH_RSA_512, false); 
                   
        KeyPair keypair = new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_512); 
        keypair.genKeyPair(); 
        pubKeyCard = (RSAPublicKey) keypair.getPublic(); 
        privKeyCard = (RSAPrivateCrtKey) keypair.getPrivate();
        
        pubKeySupermarket = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC,
        		KeyBuilder.LENGTH_RSA_512, false);
        pubKeySupermarket.setExponent(SupermarketRSAKey.getExponent(), (short) 0,
        		(short) SupermarketRSAKey.getExponent().length);
        pubKeySupermarket.setModulus(SupermarketRSAKey.getModulus(), (short) 0,
        		(short) SupermarketRSAKey.getModulus().length);
        
		sessionKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, 
				KeyBuilder.LENGTH_AES_128, false);

		rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);

		cardNonce = JCSystem.makeTransientByteArray(CONSTANTS.NONCE_LENGTH, JCSystem.CLEAR_ON_DESELECT);
		tmpKey = JCSystem.makeTransientByteArray(CONSTANTS.AES_KEY_LENGTH, JCSystem.CLEAR_ON_DESELECT);

		random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);

		authState = JCSystem.makeTransientByteArray((short) 1, JCSystem.CLEAR_ON_DESELECT);

		balance = (short) 0;

		c = card;
	}

	/**
	 * Symmetrically encrypts a plaintext into a ciphertext using a preconfigured AES session key.
	 * 
	 * @param plaintext
	 *            source buffer for the plaintext
	 * @param ptOff
	 *            offset of the plaintext in the source buffer
	 * @param ptLen
	 *            length of the plaintext in the source buffer
	 * @param ciphertext
	 *            target buffer for the ciphertext
	 * @param ctOff
	 *            offset for the ciphertext in the target buffer
	 * @return length of the ciphertext in the buffer
	 * @throws ISOException
	 *             when the card is not authenticated yet.
	 */
	short symEncrypt(byte[] plaintext, short ptOff, short ptLen, byte[] ciphertext, short ctOff) {
		if (!authenticated()) {
			Card.throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_NO_AUTH_PERFORMED);
			return 0;
		}

		verifyBufferLength(plaintext, ptOff, ptLen);
		verifyBufferLength(ciphertext, ctOff);

		// Add padding to maintain block size of 16
		Util.arrayCopyNonAtomic(plaintext, ptOff, ciphertext, (short) (ctOff + 2), ptLen);

		ciphertext[0] = (byte) (ptLen >> 8 & 0xff);
		ciphertext[1] = (byte) (ptLen & 0xff);
		ptLen += 2;

		short pad = (short) (16 - (ptLen % 16));
		if (ptOff + ptLen + pad > plaintext.length) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, CONSTANTS.SW2_SESSION_ENCRYPT_ERR);
			return 0;
		}

		Util.arrayFillNonAtomic(ciphertext, (short) (ctOff + ptLen), pad, (byte) 0);
		ptLen = (short) (ptLen + pad);

		if (ptLen % 16 != 0) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, CONSTANTS.SW2_SESSION_ENCRYPT_ERR);
			return 0;
		}

		// Generate AES key
		if (!sessionKey.isInitialized()) {
			generateSessionKey();
		}

		// Perform actual encryption
		short length = 0;
		try {
			aesCipher.init(sessionKey, Cipher.MODE_ENCRYPT);
			length = aesCipher.doFinal(ciphertext, ctOff, ptLen, ciphertext, ctOff);
		} catch (CryptoException ce) {
			c.reset();
		}
		return length;
	}

	/**
	 * Symmetrically decrypts a plaintext into a ciphertext using a preconfigured AES session key.
	 * 
	 * @see #symEncrypt(byte[], short, short, byte[], short)
	 */
	short symDecrypt(byte[] ciphertext, short ctOff, short ctLen, byte[] plaintext, short ptOff) {

		// Only use symmetric encryption when authenticated
		if (!authenticated()) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_NO_AUTH_PERFORMED);
			return 0;
		}

		// Check for buffer overflows and ciphertext misalignment

		verifyBufferLength(ciphertext, ctOff, ctLen);
		verifyBufferLength(plaintext, ptOff);

		if ((ctLen - ctOff) % 16 != 0) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, CONSTANTS.SW2_CIPHERTEXT_NOT_ALIGNED);
			return 0;
		}

		// Assume all is well and continue decrypting

		short length = 0;
		try {
			aesCipher.init(sessionKey, Cipher.MODE_DECRYPT);
			length = aesCipher.doFinal(ciphertext, ctOff, ctLen, plaintext, ptOff);
		} catch (CryptoException ce) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, (byte) ce.getReason());
		}

		length = Util.getShort(plaintext, ptOff);

		// Shift out the length bytes and strip padding bytes.
		Util.arrayCopyNonAtomic(plaintext, (short) (ptOff + 2), plaintext, ptOff, length);

		return length;
	}

	/**
	 * Encrypts a plaintext using public key cryptography.
	 * 
	 * @param key
	 *            the receiving party's public key.
	 * @param plaintext
	 *            the message to encrypt.
	 * @param ptOff
	 *            the offset for the message to encrypt.
	 * @param ptLen
	 *            the length of the message to encrypt.
	 * @param ciphertext
	 *            the target buffer for the encrypted message.
	 * @param ctOff
	 *            the offset for the ciphertext.
	 * @return the number of bytes that were encrypted.
	 * @throws ISOException
	 *             when a CryptoException is caught.
	 */
	short pubEncrypt(Key key, byte[] plaintext, short ptOff, short ptLen, byte[] ciphertext, short ctOff) {
		verifyBufferLength(plaintext, ptOff, ptLen);
		verifyBufferLength(ciphertext, ctOff);
		
		short numberOfBytes = 0;
		
		try {
			rsaCipher.init(key, Cipher.MODE_ENCRYPT);
			numberOfBytes = rsaCipher.doFinal(plaintext, ptOff, ptLen, ciphertext, ctOff);
		} catch (CryptoException ce) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, (byte) ce.getReason());
			return 0;
		}
		return numberOfBytes;
	}

	/**
	 * Decrypts a ciphertext using public key cryptography.
	 * 
	 * @see #pubEncrypt(Key, byte[], short, short, byte[], short)
	 */
	short pubDecrypt(byte[] ciphertext, short ctOff, short ctLen, byte[] plaintext, short ptOff) {
		verifyBufferLength(ciphertext, ctOff, ctLen);
		verifyBufferLength(plaintext, ptOff);
		
		short numberOfBytes = 0;

		try {
			rsaCipher.init(privKeyCard, Cipher.MODE_DECRYPT);
			numberOfBytes = rsaCipher.doFinal(ciphertext, ctOff, ctLen, plaintext, ptOff);
		} catch (CryptoException ce) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, (byte) ce.getReason());
			return 0;
		}

		return numberOfBytes;
	}

	/**
	 * Generates the AES session key. This key is used until the card is removed from the terminal.
	 */
	void generateSessionKey() {
		fillRandom(tmpKey);
		sessionKey.setKey(tmpKey, (short) 0);

		// Clear the temporary buffer holding the key.
		Util.arrayFillNonAtomic(tmpKey, (short) 0, (short) tmpKey.length, (byte) 0);
	}

	/**
	 * Generates a nonce for use during authentication.
	 */
	void generateCardNonce() {
		fillRandom(cardNonce);
	}

	/**
	 * Compares the nonce in <code>buffer</code> with the one stored internally in <code>cardNonce</code>.
	 * 
	 * @param buffer
	 *            the buffer holding the nonce to be checked.
	 * @param offset
	 *            the offset in the buffer where the nonce is located.
	 * @return <code>true</code> if the nonces match.<br />
	 *         <code>false</code> if the nonces do not match.
	 */
	boolean checkCardNonce(byte[] buffer, short offset) {
		return Util.arrayCompare(buffer, CONSTANTS.AUTH_MSG_3_OFFSET_NC, cardNonce, (short) 0, CONSTANTS.NONCE_LENGTH) == 0;
	}

	/**
	 * Fill an entire buffer with random values.
	 * 
	 * @see javacard.security.RandomData#generateData(byte[], short, short)
	 * @param buf
	 *            the buffer to be filled.
	 */
	private void fillRandom(byte[] buf) {
		random.generateData(buf, (short) 0, (short) buf.length);
	}

	/**
	 * Clears all session-related data from the Crypto object.
	 */
	void clearSessionData() {
		sessionKey.clearKey();
		Util.arrayFillNonAtomic(tmpKey, (short) 0, (short) tmpKey.length, (byte) 0);
		Util.arrayFillNonAtomic(cardNonce, (short) 0, (short) cardNonce.length, (byte) 0);
		disable();
	}

	/**
	 * Checks for possible buffer overflows and throws an exception in that case.
	 * 
	 * @param buf
	 *            the buffer to check for overflows.
	 * @param offset
	 *            the offset of the buffer.
	 * @throws ISOException
	 *             when a buffer overflow might occur with these parameters.
	 */
	private void verifyBufferLength(byte[] buf, short offset) {
		if (offset < 0 || offset >= buf.length) {
			Card.throwException(CONSTANTS.SW1_NO_PRECISE_DIAGNOSIS, CONSTANTS.SW2_INTERNAL_ERROR);
		}
	}

	/**
	 * Checks for possible buffer overflows and throws an exception in that case.
	 * 
	 * @param buf
	 *            the buffer to check for overflows.
	 * @param offset
	 *            the offset of the buffer.
	 * @param length
	 *            the length of the buffer.
	 * @throws ISOException
	 *             when a buffer overflow might occur with these parameters
	 */
	private void verifyBufferLength(byte[] buf, short offset, short length) {
		if (offset < 0 || length < 0 || offset + length >= buf.length) {
			Card.throwException(CONSTANTS.SW1_NO_PRECISE_DIAGNOSIS, CONSTANTS.SW2_INTERNAL_ERROR);
		}
	}

	/**
	 * Spend an amount of credits at a terminal.
	 * 
	 * @param amount
	 *            The amount of credits (>= 0) required for the purchase.
	 * @return 1 if all went well, 0 if an exception occurred.
	 * @throws ISOException
	 *             when the current balance is less than the amount that is being spent.
	 */
	short spend(short amount) {
		if (amount < 0 || balance < amount) {
			Card.throwException(CONSTANTS.SW1_WRONG_PARAMETERS, CONSTANTS.SW2_CREDITS_INSUFFICIENT);
			return balance;
		} else {
			balance -= amount;
			return balance;
		}
	}

	/**
	 * Gain an amount of credits from shopping for groceries.
	 * 
	 * @param amount
	 *            the amount by which to increase the balance.
	 * @return the new balance.
	 * @throws ISOException
	 *             when <code>amount</code> is negative.
	 */
	short gain(short amount) {
		if (amount < 0) {
			Card.throwException(CONSTANTS.SW1_WRONG_PARAMETERS, CONSTANTS.SW2_CREDITS_NEGATIVE);
			return balance;
		} else {
			balance += amount;
			return balance;
		}
	}

	/**
	 * Checks the authentication status.
	 * 
	 * @return <code>true</code> if the card has previously authenticated to a terminal.<br />
	 *         <code>false</code>otherwise.
	 */
	boolean authenticated() {
		return authState[0] == CONSTANTS.SESSION_ESTABLISHED;
	}

	/**
	 * Enables the cryptographic operations of <code>this</code> card. This makes it possible to make changes in the balance and retrieve it.
	 */
	void enable() {
		authState[0] = CONSTANTS.SESSION_ESTABLISHED;
	}

	/**
	 * Disables the cryptographic operations of <code>this</code> card. This makes it impossible to make any changes in the balance or even retrieve it.
	 */
	void disable() {
		authState[0] = CONSTANTS.NO_ACTIVE_SESSION;
	}

	/**
	 * Changes the card to the <code>ISSUED</code> state.
	 * 
	 * @return 1 if the card was issued successfully<br />
	 *         0 if an error occurred.
	 * @throws ISOException
	 *             if the card has already been issued.
	 */
	short issueCard() {
		if (c.state == CONSTANTS.STATE_ISSUED) {
			Card.throwException(CONSTANTS.SW1_COMMAND_NOT_ALLOWED_00, CONSTANTS.SW2_ALREADY_ISSUED);
			return 0;
		} else {
			c.state = CONSTANTS.STATE_ISSUED;
			return 1;      
		}
	}

	/**
	 * Returns the current balance of the card, if authenticated.
	 * 
	 * @return the current balance.
	 * @throws ISOException
	 *             if the user is not authenticated yet.
	 */
	short getBalance() {
		if (!authenticated()) {
			Card.throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_NO_AUTH_PERFORMED);
			return 0;
		} else {
			return balance;
		}
	}

	/**
	 * Retrieves <code>this</code> card's name.
	 * 
	 * @param buffer
	 *            the buffer to hold the card's name.
	 * @param offset
	 *            the offset in the buffer.
	 * @return length of the name, 0 if an exception occurred.
	 */
	short getCardName(byte[] buffer, short offset) {
		try {
			Util.arrayCopyNonAtomic(CONSTANTS.NAME_CARD, (short) 0, buffer, offset, CONSTANTS.NAME_LENGTH);
		} catch (Exception e) {
			Card.throwException(((CardRuntimeException) e).getReason());
			return 0;
		}
		return CONSTANTS.NAME_LENGTH;
	}

	/**
	 * Retrieves <code>this</code> card's nonce. Must be initialized before retrieval.
	 * 
	 * @param buffer
	 *            the buffer to hold the nonce.
	 * @param offset
	 *            the offset in the buffer.
	 * @return the amount of bytes copied into the buffer, 0 if the nonce was not initialized.
	 */
	short getCardNonce(byte[] buffer, short offset) {
		return Util.arrayCopyNonAtomic(cardNonce, (short) 0, buffer, offset, CONSTANTS.NONCE_LENGTH);
	}

	/**
	 * Gets the session key
	 * 
	 * @param buffer
	 *            the buffer to hold the key.
	 * @param offset
	 *            the offset in the buffer to place the key.
	 * @return the byte length of the key data returned (16, 24 or 36)
	 * @see AESKey#getKey(byte[], short)
	 */
	short getSessionKey(byte[] buffer, short offset) {
		short len = 0;
		try {
			len = sessionKey.getKey(buffer, offset);
		} catch (CryptoException ce) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_SECURITY_RELATED_ISSUE_00, (byte) ce.getReason()); // key not init
			return 0;
		}
		return len;
	}

	/**
	 * Gets the public key of <code>this</code> card. The key returned contains the exponent of the public key and the modulus
	 * of the keypair, respectively.
	 * 
	 * @param buf
	 *            the buffer to hold the key.
	 * @param offset
	 * 			  the offset in the buffer to place the key
	 * @return the length of the data in the buffer.
	 */
	public short getPubKeyCard(byte[] buf, short offset) {
		short totalLength = 0;
		if (!pubKeyCard.isInitialized()) {
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, CONSTANTS.SW2_AUTH_PARTNER_KEY_NOT_INIT);
			return 0;
		} else if (buf.length < CONSTANTS.RSA_PUBKEY_LENGTH) {
			Card.throwException(CONSTANTS.SW1_WRONG_LENGTH);
			return 0;
		} else {
			totalLength += pubKeyCard.getExponent(buf, (short) (CONSTANTS.PUB_KEY_CARD_EXP_OFF + offset));
			totalLength += pubKeyCard.getModulus(buf, (short) (CONSTANTS.PUB_KEY_CARD_MOD_OFF + offset));
			return totalLength;
		}
		
	}
	
	/**
	 * Retrieves the supermarket's public key to use for encryption.
	 * 
	 * @return the supermarket's public key.
	 * @throws ISOException
	 *             if the supermarket's public key is not initialized yet.
	 */
	RSAPublicKey getPubKeySupermarket() {
		if (!pubKeySupermarket.isInitialized()) {
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, CONSTANTS.SW2_AUTH_PARTNER_KEY_NOT_INIT);
			return null;
		} else {
			return pubKeySupermarket;
		}
	}
}
