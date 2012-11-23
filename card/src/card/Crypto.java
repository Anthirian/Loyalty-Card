package card;

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

	byte[] symEncrypt(byte[] plaintext) {
		byte[] ciphertext;
		return ciphertext;
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
	 * @param amount The amount of credits (>= 0) required for the purchase.
	 * @return The new balance on the card.
	 */
	short spend(short amount) {
		if (amount >= 0 && balance >= amount) {
			balance -= amount;
		} 
		
		return balance;
	}
}
