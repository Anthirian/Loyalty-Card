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

	private RSAPublicKey pubKeyCompany;

	private RSAPublicKey pubKeyCar;

	private byte[] authState;

	private byte[] cert;
	private byte[] carID;

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
		pubKeyCompany = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		pubKeyCompany.setExponent(CompanyRSAPublicKey.getExponent(), (short) 0, (short) CompanyRSAPublicKey.getExponent().length);
		pubKeyCompany.setModulus(CompanyRSAPublicKey.getModulus(), (short) 0, (short) CompanyRSAPublicKey.getModulus().length);

		pubKeyCar = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);

		privKeyCard = (RSAPrivateCrtKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_CRT_PRIVATE, KeyBuilder.LENGTH_RSA_1024, false);
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

		authState = JCSystem.makeTransientByteArray((short) 1, JCSystem.CLEAR_ON_DESELECT);

		cert = new byte[CONSTANTS.CERT_LENGTH];
		carID = new byte[CONSTANTS.ID_LENGTH];

		balance = new byte[CONSTANTS.MILEAGE_MSG_LENGTH];

		c = card;
	}

	boolean checkSignature() {
		return false;
	}

	short createSignature() {
		return 0;
	}

	/**
	 * Symmetrically encrypts a plaintext into a ciphertext using a preconfigured AES key.
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
	 */
	short symEncrypt(byte[] plaintext, short ptOff, short ptLen, byte[] ciphertext, short ctOff) {
		if (!c.isAuthenticated()) {
			Card.throwException(CONSTANTS.SW1_AUTH_EXCEPTION, CONSTANTS.SW2_NO_AUTH_PERFORMED);
			return 0;
		}
		
		// More checks needed

		short length = 0;
		try {
			// Do the actual encryption here
		} catch (Exception e) {
			// Catch a meaningful exception, not just any.
		}
		return length;
	}

	byte[] symDecrypt(byte[] ciphertext) {
		byte[] plaintext;
		return plaintext;
	}

	byte[] pubEncrypt(byte[] plaintext) {
		byte[] ciphertext;
		return ciphertext;
	}

	byte[] pubDecrypt(byte[] ciphertext) {
		byte[] plaintext;
		return plaintext;
	}

	/**
	 * Spend an amount of credits at a terminal.
	 * 
	 * @param amount
	 *            The amount of credits (>= 0) required for the purchase.
	 * @return The new balance on the card.
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
	 * @return a <code>short</code> containing the new balance after increase. The short should make ensure only positive values.
	 */
	short gain(short amount) {
		if (amount >= 0) {
			balance += amount;
		} else {
			// Throw exception with appropriate SW
		}
		return balance;
	}
}
