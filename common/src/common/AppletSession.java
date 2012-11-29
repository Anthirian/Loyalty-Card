package common;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;


public class AppletSession {
	
	private TerminalCrypto crypto;
	private AppletCommunication com;
	
	private RSAPublicKey pubKeySupermarket;
	private RSAPublicKey pubKeyCard;
	private RSAPrivateKey privKey;
	
	private int terminalId;
	private int cardId;
	
	private byte[] sessionKey;
	private boolean authenticationSuccess;
	
	public AppletSession(RSAPublicKey pubKeySupermarket, RSAPrivateKey privKey,
			int terminalId) {
		this.pubKeySupermarket = pubKeySupermarket;
		this.terminalId = terminalId;
		this.privKey = privKey;
		this.crypto = new TerminalCrypto();
		this.reset();
	}
	
	public void setAppletCommunication(AppletCommunication com) {
		this.com = com;
	}
	
	public int getCardId() {
		return cardId;
	}
	
	public RSAPublicKey getCardPublicKey() {
		return pubKeyCard;
	}
	
	public RSAPrivateKey getPrivateKey() {
		return privKey;
	}
	
	public boolean isAuthenticated() {
		return authenticationSuccess;
	}
	
	public byte[] getSessionKey() {
		return sessionKey;
	}
	
	public void reset() {
		this.cardId = 0;
		this.pubKeyCard = null;
		this.authenticationSuccess = false;
		this.sessionKey = null;
	}
	
	// handshake protocol
	public boolean authenticate(byte from) {
		try {
			// initiate authentication
			byte[] nonceCard = initiateAuthentication(from);

			// when there is no correct message send the nonce is null
			if (nonceCard != null) {

				// generate new random nonce
				byte[] nonceTerminal = crypto
						.generateRandomNonce(CONSTANTS.NONCE_LENGTH);

				// send received and generated nonce to the card
				// receive the generated nonce and a certificate
				byte[] nonceReceived = authenticateTerminal(from, nonceCard,
						nonceTerminal);

				// when there is no correct message send the nonce is null
				if (nonceReceived != null) {

					// check the nonces to authenticate the card
					if (authenticateCard(nonceReceived, nonceTerminal)) {
						System.out.println("Authenticated.");
						sessionKey = new byte[CONSTANTS.AES_KEY_LENGTH];
						System.arraycopy(nonceCard, 0, sessionKey, 0,
								CONSTANTS.NONCE_LENGTH);
						System.arraycopy(nonceTerminal, 0, sessionKey,
								CONSTANTS.NONCE_LENGTH,
								CONSTANTS.NONCE_LENGTH);
						authenticationSuccess = true;
						return true;
					} else {
						System.out.println("Authentication failure.");
					}
				}
			}
		} catch (SecurityException e) {
			reset();
			// System.err.println(e.getMessage());
		}
		return false;
	}
	
	private byte[] initiateAuthentication(byte from) {
		// send authentication apdu

		Response response;
		try {
			response = com.sendCommand(CONSTANTS.INS_AUTHENTICATE, from,
					CONSTANTS.P2_AUTHENTICATE_STEP1);
		} catch (Exception e) {
			throw new SecurityException();
		}

		if (response == null) {
			throw new SecurityException();
		}

		if (!response.success()) {
			throw new SecurityException();
		}

		// decrypt the data with own private key
		byte[] data = crypto.decrypt(response.getData(), this.privKey);

		if (data == null) {
			throw new SecurityException();
		}

		// extract the card nonce
		byte[] cardNonce = Arrays.copyOfRange(data,
				CONSTANTS.AUTH_MSG_1_OFFSET_NA,
				CONSTANTS.AUTH_MSG_1_OFFSET_NA + CONSTANTS.NONCE_LENGTH);

		// extract the signed public key - id pair
		byte[] cert = Arrays.copyOfRange(data,
				CONSTANTS.AUTH_MSG_1_OFFSET_SIGNED_PUBKEY,
				CONSTANTS.AUTH_MSG_1_OFFSET_SIGNED_PUBKEY
						+ CONSTANTS.RSA_SIGNED_PUBKEY_LENGTH);

		try {
			data = crypto.verify(cert, pubKeySupermarket);
		} catch (SignatureException e) {
			throw new SecurityException();
		}

		// save the card id
		byte[] idCardBytes = Arrays.copyOfRange(data,
				CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_ID,
				CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_ID + CONSTANTS.ID_LENGTH);
		ByteBuffer bb = ByteBuffer.wrap(idCardBytes);
		this.cardId = bb.getInt();

		// save the card public key
		BigInteger exponent = new BigInteger(1, Arrays.copyOfRange(data,
				CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_EXP,
				CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_EXP
						+ CONSTANTS.RSA_KEY_PUBEXP_LENGTH));
		BigInteger modulus = new BigInteger(1, Arrays.copyOfRange(data,
				CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_MOD,
				CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_MOD
						+ CONSTANTS.RSA_KEY_MOD_LENGTH));
		RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, exponent);

		try {
			KeyFactory factory = KeyFactory.getInstance("RSA");
			this.pubKeyCard = (RSAPublicKey) factory.generatePublic(pubKeySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new SecurityException(e.getMessage());
		} catch (InvalidKeySpecException e) {
			throw new SecurityException();
		}

		return cardNonce;
	}
	
	private byte[] authenticateTerminal(byte from, byte[] nonceCard,
			byte[] nonceTerminal) {
		// build the message to be sent to the card
		byte[] data = new byte[CONSTANTS.AUTH_MSG_2_LENGTH];
		System.arraycopy(nonceCard, 0, data, CONSTANTS.AUTH_MSG_2_OFFSET_NA,
				CONSTANTS.NONCE_LENGTH);
		System.arraycopy(nonceTerminal, 0, data,
				CONSTANTS.AUTH_MSG_2_OFFSET_NB, CONSTANTS.NONCE_LENGTH);
		byte[] terminalIdBytes = ByteBuffer.allocate(CONSTANTS.SEQNR_BYTESIZE).
				putInt(this.terminalId).array();
		System.arraycopy(terminalIdBytes, 0, data,
				CONSTANTS.AUTH_MSG_2_OFFSET_ID, CONSTANTS.ID_LENGTH);

		// encrypt the message with the card's public key
		data = crypto.encrypt(data, this.pubKeyCard);

		// send the command
		Response response;
		try {
			response = com.sendCommand(CONSTANTS.INS_AUTHENTICATE, from,
					CONSTANTS.P2_AUTHENTICATE_STEP2, data);
		} catch (Exception e) {
			throw new SecurityException();
		}

		if (!response.success()) {
			throw new SecurityException();
		}

		// decrypt the response with the terminal private key
		data = crypto.decrypt(response.getData(), this.privKey);

		if (data == null) {
			throw new SecurityException();
		}

		// extract the received nonce 
		byte[] nonceReceived = Arrays.copyOfRange(data,
				CONSTANTS.AUTH_MSG_3_OFFSET_NB,
				CONSTANTS.AUTH_MSG_3_OFFSET_NB + CONSTANTS.NONCE_LENGTH);
		
		return nonceReceived;
	}
	
	private boolean authenticateCard(byte[] nonceReceived, byte[] nonceTerminal) {
		// check if the card can successfully decrypt the message
		// encrypted with the cards' public key
		return Arrays.equals(nonceReceived, nonceTerminal);
	}
}