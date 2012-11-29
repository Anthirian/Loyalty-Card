package card;

import javacard.framework.CardRuntimeException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.CryptoException;
import javacard.security.Key;
import javacard.security.KeyBuilder;
import javacard.security.RSAPrivateCrtKey;
import javacard.security.RandomData;
import javacard.security.RSAPublicKey;
import javacardx.crypto.Cipher;
import javacard.security.Signature;

import common.CONSTANTS;

/**
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public final class Crypto {

	private Cipher rsaCipher;
	private Cipher aesCipher;

	private Signature rsaSignature;

	private RandomData random;
	private byte[] cardNonce;
	private byte[] termNonce;
	private byte[] tmpKey;
	private AESKey sessionKey;
	private AESKey messageKey;

	private byte[] pubKeyCard;
	private RSAPrivateCrtKey privKeyCard;

	/** The public key of the supermarket */
	private RSAPublicKey pubKeyCompany;
	private RSAPublicKey pubKeyCar;

	/** The state of authentication of this card, an array of size one */
	private byte[] authState;

	private byte[] cert;
	private byte[] carID;

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
		pubKeyCompany = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);
		pubKeyCompany.setExponent(SupermarketRSAKey.getExponent(), (short) 0, (short) SupermarketRSAKey.getExponent().length);
		pubKeyCompany.setModulus(SupermarketRSAKey.getModulus(), (short) 0, (short) SupermarketRSAKey.getModulus().length);

		pubKeyCar = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);

		privKeyCard = (RSAPrivateCrtKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_CRT_PRIVATE, KeyBuilder.LENGTH_RSA_512, false);
		pubKeyCard = new byte[CONSTANTS.RSA_SIGNED_PUBKEY_LENGTH];

		sessionKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_128, false);
		messageKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT, KeyBuilder.LENGTH_AES_128, false);

		rsaCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		rsaSignature = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);

		cardNonce = JCSystem.makeTransientByteArray(CONSTANTS.NONCE_LENGTH, JCSystem.CLEAR_ON_DESELECT);
		termNonce = JCSystem.makeTransientByteArray(CONSTANTS.NONCE_LENGTH, JCSystem.CLEAR_ON_DESELECT);
		tmpKey = JCSystem.makeTransientByteArray(CONSTANTS.AES_KEY_LENGTH, JCSystem.CLEAR_ON_DESELECT);

		random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);

		authState = JCSystem.makeTransientByteArray((short) 0, JCSystem.CLEAR_ON_DESELECT);

		cert = new byte[CONSTANTS.CERT_LENGTH];
		carID = new byte[CONSTANTS.ID_LENGTH];

		balance = (short) 0;

		c = card;
	}

	boolean checkSignature() {
		return false;
	}

	/**
	 * Signs a message with the private key of <code>this</code> card.
	 * 
	 * @param data
	 *            the data to be signed.
	 * @param dataOff
	 *            the offset of the data to be signed.
	 * @param dataLen
	 *            the length of the data to be signed.
	 * @param sig
	 *            the resulting signature.
	 * @param sigOffset
	 *            the offset of the resulting signature.
	 * @return the length of the resulting signature in bytes.
	 */
	short sign(byte[] data, short dataOff, short dataLen, byte[] sig, short sigOffset) {
		try {
			rsaSignature.init(privKeyCard, Signature.MODE_SIGN);
			return rsaSignature.sign(data, dataOff, dataLen, sig, sigOffset);
		} catch (CryptoException ce) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, (byte) ce.getReason());
			return 0;
		}
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

	short symDecrypt(byte[] ciphertext, short ctOff, short ctLen, byte[] plaintext, short ptOff) {
		// TODO AES Decryption not finished yet

		short length = 0;
		try {
			aesCipher.init(sessionKey, Cipher.MODE_DECRYPT);
			length = aesCipher.doFinal(ciphertext, ctOff, ctLen, plaintext, ptOff);
		} catch (CryptoException ce) {
			c.reset();
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, (byte) ce.getReason());
		}
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
	 * @param ciphertext
	 * @param ctOff
	 * @param ctLen
	 * @param plaintext
	 * @param ptOff
	 * @return Length of decrypted message in <code>plaintext</code>. Will be 0 if decryption fails.
	 * @throws ISOException
	 *             when a CryptoException is caught.
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
	private void generateSessionKey() {
		fillRandom(tmpKey);
		sessionKey.setKey(tmpKey, (short) 0);

		// Clear the temporary buffer holding the key.
		Util.arrayFillNonAtomic(tmpKey, (short) 0, (short) tmpKey.length, (byte) 0);
	}

	/**
	 * Generates a nonce for use during authentication.
	 * 
	 * @param buf
	 *            the buffer in which to store the nonce.
	 */
	void generateCardNonce() {
		fillRandom(cardNonce);
	}

	/**
	 * Fill an entire buffer with random values.
	 * 
	 * @see {@link javacard.security.RandomData#generateData(byte[], short, short) generateData}
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
		messageKey.clearKey();
		Util.arrayFillNonAtomic(tmpKey, (short) 0, (short) tmpKey.length, (byte) 0);
		Util.arrayFillNonAtomic(cardNonce, (short) 0, (short) cardNonce.length, (byte) 0);
		Util.arrayFillNonAtomic(termNonce, (short) 0, (short) termNonce.length, (byte) 0);
		authState[0] = 0;
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
	 * @return The new balance on the card.
	 * @throws ISOException
	 *             when the current balance is less than the amount that is being spent.
	 */
	short spend(short amount) {
		if (amount >= 0 && balance >= amount) {
			balance -= amount;
		} else {
			Card.throwException(CONSTANTS.SW1_WRONG_PARAMETERS, CONSTANTS.SW2_CREDITS_INSUFFICIENT);
		}

		return balance;
	}

	/**
	 * Gain an amount of credits from shopping for groceries.
	 * 
	 * @param amount
	 *            the amount by which to increase the balance.
	 * @return the new balance after increase.
	 * @throws ISOException
	 *             when <code>amount</code> is negative.
	 */
	short gain(short amount) {
		if (amount >= 0) {
			balance += amount;
		} else {
			Card.throwException(CONSTANTS.SW1_WRONG_PARAMETERS, CONSTANTS.SW2_CREDITS_NEGATIVE);
		}
		return balance;
	}

	/**
	 * Returns the current balance of the card, if authenticated.
	 * 
	 * @return the current balance.
	 * @throws ISOException
	 *             if the user is not authenticated yet.
	 */
	short getBalance() {
		if (authenticated()) {
			return balance;
		} else {
			Card.throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_NO_AUTH_PERFORMED);
			return 0;
		}
	}

	/**
	 * Checks the authentication status.
	 * 
	 * @return <code>true</code> if the card has previously authenticated to a terminal.<br />
	 *         <code>false</code>otherwise.
	 */
	boolean authenticated() {
		return authState[0] == 1;
	}
	
	/**
	 * Retrieves <code>this</code> card's public key to use for encryption.
	 * 
	 * @return <code>this</code> card's public key.
	 */
	public Key getCardKey() {
		if (pubKeyCar.isInitialized()) {
			return pubKeyCar;
		} else {
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, CONSTANTS.SW2_AUTH_PARTNER_KEY_NOT_INIT);
			return null;
		}
	}

	/**
	 * Retrieves the supermarket's public key to use for encryption.
	 * 
	 * @return the supermarket's public key.
	 * @throws ISOException
	 *             if the supermarket's public key is not initialized yet.
	 */
	Key getCompanyKey() {
		if (pubKeyCompany.isInitialized()) {
			return pubKeyCompany;
		} else {
			Card.throwException(CONSTANTS.SW1_CRYPTO_EXCEPTION, CONSTANTS.SW2_AUTH_PARTNER_KEY_NOT_INIT);
			return null;
		}
	}
}
