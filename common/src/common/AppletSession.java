package common;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

/**
 * Class representing a session between the card and the terminal
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class AppletSession {

	private TerminalCrypto crypto;
	private AppletCommunication com;

	private RSAPublicKey pubKeyCard;
	private RSAPrivateKey privKey;

	private byte[] cardId;
	
	private byte[] sessionKey;
	private boolean authenticationSuccess;

	public AppletSession(RSAPrivateKey privKey) {
		this.privKey = privKey;
		this.crypto = new TerminalCrypto();
		this.reset();
	}

	public void setAppletCommunication(AppletCommunication com) {
		this.com = com;
	}

	public byte[] getCardId() {
		return cardId;
	}
	
	public int getCardIdAsInt() {
		return Formatter.byteArrayToInt(cardId);
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

	/**
	 * Resets the current session
	 */
	public void reset() {
		this.cardId = null;
		this.pubKeyCard = null;
		this.authenticationSuccess = false;
		this.sessionKey = null;
	}

	/**
	 * Handshake protocol authenticating the terminal to the card and vice versa
	 * @param from
	 * @return
	 */
	public boolean authenticate(byte[] from) {
		try {
			setPubKeyCard();
			
			// initiate authentication
			byte[] nonceCard = authStep1(from);
			
			// when there is no correct message sent, the nonce is null
			if (nonceCard != null) {

				// generate new random nonce
				byte[] nonceTerminal = crypto.generateRandomNonce(CONSTANTS.NONCE_LENGTH);

				// send received and generated nonce to the card
				// receive the generated nonce
				byte[] sessionKey = authStep3(from, cardId, nonceCard, nonceTerminal);

				// when there is no correct message sent, the nonce is null
				if (sessionKey != null) {
						authenticationSuccess = true;
						return true;
					} else {
						System.out.println("Authentication failure.");
					}
				}
		} catch (SecurityException e) {
			reset();
			System.err.println(e.getMessage());
		}
		return false;
	}
	
	/**
	 * Request the public key from the card, needed for encryption
	 * @return the public key of the card as byte array of modulus and exponent
	 */
	private byte[] setPubKeyCard() {
		Response response;
		try {
			response = com.sendCommand(CONSTANTS.INS_GET_PUBKEY);
		} catch (Exception e) {
			throw new SecurityException(e.getMessage());
		}
		
		if (response == null) {
			throw new SecurityException("Empty response");
		}
		
		if (!response.success()) {
			throw new SecurityException("Response failed");
		}
		
		byte[] data = response.getData();
		
		if (data == null) {
			throw new SecurityException("Empty data");
		}

		// save the card's public key
		BigInteger exponent = new BigInteger(1, Arrays.copyOfRange(data,
				CONSTANTS.PUB_KEY_CARD_EXP_OFF, CONSTANTS.PUB_KEY_CARD_EXP_OFF
				+ CONSTANTS.RSA_KEY_PUBEXP_LENGTH));
		BigInteger modulus = new BigInteger(1, Arrays.copyOfRange(data,
				CONSTANTS.PUB_KEY_CARD_MOD_OFF, CONSTANTS.PUB_KEY_CARD_MOD_OFF
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
		
		return data;
	}

	/**
	 * First step of the handshake protocol
	 * @param from the terminal
	 * @return a nonce received from the card
	 */
	private byte[] authStep1 (byte[] from) {
		// send authentication apdu		
		byte[] sendData = new byte[CONSTANTS.AUTH_MSG_1_TOTAL_LENGTH];
		System.arraycopy(from, 0, sendData, CONSTANTS.AUTH_MSG_1_OFFSET_NAME_TERM, 
				CONSTANTS.NAME_LENGTH);
		
		Response response;
		try {
			response = com.sendCommand(CONSTANTS.INS_AUTHENTICATE, 
					CONSTANTS.P1_AUTHENTICATE_CARD, CONSTANTS.P2_AUTHENTICATE_STEP1, sendData);
			//response = com.sendCommand(CONSTANTS.INS_GET_PUBKEY);
			//response = com.sendCommand(CONSTANTS.INS_ISSUE);
		} catch (Exception e) {
			throw new SecurityException();
		}
		
		if (response == null) {
			throw new SecurityException("Empty response");
		}

		if (!response.success()) {
			throw new SecurityException();
		}
		
		// decrypt the data with own private key
		byte[] data = crypto.decryptRSA(response.getData(), this.privKey);
		
		if (data == null) {
			throw new SecurityException("Empty data");
		}

		// extract the card name + nonce
		this.cardId = Arrays.copyOfRange(data, CONSTANTS.AUTH_MSG_2_OFFSET_NAME_CARD, 
				CONSTANTS.AUTH_MSG_2_OFFSET_NAME_TERM);
		byte[] cardNonce = Arrays.copyOfRange(data, CONSTANTS.AUTH_MSG_2_OFFSET_NC, 
				CONSTANTS.AUTH_MSG_2_OFFSET_NC + CONSTANTS.NONCE_LENGTH);
		byte[] receivedTerminalName = Arrays.copyOfRange(data, CONSTANTS.AUTH_MSG_2_OFFSET_NAME_TERM,
				CONSTANTS.AUTH_MSG_2_OFFSET_NAME_TERM + CONSTANTS.NAME_LENGTH);
		
		// verify decrypted_challenge[1] equals terminal name
		if (!(Arrays.equals(receivedTerminalName,CONSTANTS.NAME_TERM))) {
			throw new SecurityException("Terminal name on card does not match registered terminal name");
		}
				
		return cardNonce;
	}

	/**
	 * Third step (second for the terminal) of the handshake protocol
	 * @param from the terminal
	 * @param nameCard name of the card
	 * @param nonceCard nonce received during step 1
	 * @param nonceTerminal nonce generated by the terminal
	 * @return
	 */
	private byte[] authStep3(byte[] from, byte[] nameCard, byte[] nonceCard, 
			byte[] nonceTerminal) {
		// build the message to be sent to the card
		byte[] data = new byte[CONSTANTS.AUTH_MSG_3_TOTAL_LENGTH];
		System.arraycopy(from, 0, data, CONSTANTS.AUTH_MSG_3_OFFSET_NAME_TERM, 
				CONSTANTS.NAME_LENGTH);
		System.arraycopy(nameCard, 0, data, CONSTANTS.AUTH_MSG_3_OFFSET_NAME_CARD, 
				CONSTANTS.NAME_LENGTH);
		System.arraycopy(nonceCard, 0, data, CONSTANTS.AUTH_MSG_3_OFFSET_NC, 
				CONSTANTS.NONCE_LENGTH);
		System.arraycopy(nonceTerminal, 0, data, CONSTANTS.AUTH_MSG_3_OFFSET_NT, 
				CONSTANTS.NONCE_LENGTH);
				
		// encrypt the message with the card's public key
		data = crypto.encryptRSA(data, this.pubKeyCard);

		// send the command
		Response response;
		try {
			response = com.sendCommand(CONSTANTS.INS_AUTHENTICATE,
					CONSTANTS.P1_AUTHENTICATE_CARD, CONSTANTS.P2_AUTHENTICATE_STEP2, data);
		} catch (Exception e) {
			throw new SecurityException();
		}

		if (!response.success()) {
			throw new SecurityException();
		}
		
		// decrypt the response with the terminal private key
		data = crypto.decryptRSA(response.getData(), this.privKey);

		if (data == null) {
			throw new SecurityException();
		}

		// extract the received nonce
		byte[] nonceReceived = Arrays.copyOfRange(data, CONSTANTS.AUTH_MSG_4_OFFSET_NT, 
				CONSTANTS.AUTH_MSG_4_OFFSET_NT + CONSTANTS.NONCE_LENGTH);

		// when there is no correct message sent, the nonce is null
		if (nonceReceived != null) {

			// check the nonces to authenticate the card, and store the session key
			if (authenticateCard(nonceReceived, nonceTerminal)) {
				System.out.println("Authenticated.");
				sessionKey = new byte[CONSTANTS.AES_KEY_LENGTH];
				System.arraycopy(data, CONSTANTS.AUTH_MSG_4_OFFSET_SESSION_KEY, 
						sessionKey, 0, CONSTANTS.AES_KEY_LENGTH);				
			} else {
				System.out.println("Authentication failure.");
			}
		}
		
		return sessionKey;
	}

	private boolean authenticateCard(byte[] nonceReceived, byte[] nonceTerminal) {
		// check if the card can successfully decrypt the message
		// encrypted with the cards' public key
		return Arrays.equals(nonceReceived, nonceTerminal);
	}
	
}