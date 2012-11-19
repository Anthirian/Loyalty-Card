package card;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SecureRandom;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// We need this JCE provider because it has more features than the regular one
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Java Card applet to be used for the Loyalty Card system
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
	 * States of the card. If the card is in the initialization state, 
	 * the crypto keys can be written. If the card has been issued, 
	 * the keys can be used for cryptography.
	 */
	private static final byte STATE_INIT = 0;
	private static final byte STATE_ISSUED = 1;
	
	/** Temporary buffer in RAM. */
	byte[] tmp;
	
	/** The applet state (INIT or ISSUED). */
	byte state;
	
	/** Public Key of the Card. Used for encryption. */
	private final Key pkC;
	/** Secret Key of the Card. Used for decryption. */
	private final Key skC;
	/** Public Key of the Terminal. Used for encryption */
	private final Key pkT;
	/** Cipher for encryption and decryption. */
	private final Cipher cipher;
	
	private short balance = -1;
	SecureRandom random;
	
	public Card () {
		
		// Check if the card has already been initialized. If so, don't do it again
		if (state == STATE_INIT) {			
		
			// Dit levert problemen op zonder import.
			// Zodra je de Security import doet en de JDK 6 toevoegt aan build path is de packagenaam niet ok.
			// Wel nodig omdat standaard crypto van Java niet genoeg is volgens website Erik
			Security.addProvider(new BouncyCastleProvider());
			
			tmp = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
			// pkC = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);
			// skC = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_512, false);
			// cipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
			
			// pkT = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);
			
			// Generate key pair using the Bouncy Castle (BC) security provider
			KeyPairGenerator kpg;
			
			random = new SecureRandom();
			byte[] bytes = new byte[64]; // 512 bits
			random.nextBytes(bytes);
			
			try {
				Cipher cipher = Cipher.getInstance("RSA/NONE/NoPadding", "BC");
				kpg = KeyPairGenerator.getInstance("RSA", "BC");
	
				kpg.initialize(512, random);
				
				KeyPair kp = kpg.generateKeyPair();
				Key pkC = kp.getPublic();
				Key skC = kp.getPrivate();
			} catch (NoSuchAlgorithmException e) {
				// TODO Report authentication failure
				return;
			} catch (NoSuchProviderException e) {
				// TODO Report authentication failure
				return;
			} catch (NoSuchPaddingException e) {
				// TODO Report authentication failure
				return;
			}
			
			// Set the state of the card to initialization to allow for key generation and uploading
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
		short value = -1;
		
		byte[] buf = apdu.getBuffer();
		short lc = (short) (buf[OFFSET_LC] & 0x00FF);
		byte ins = buf[ISO7816.OFFSET_INS];
		
		// Decide what to do based on the instruction byte
		switch(state) {
        case STATE_INIT:
        	// When initializing we have several options to set cryptographic keys etc.
        	switch (ins) {
        	/*
        	 * The first two instruction are to save the parts of the public key of the terminal to memory.
        	 */
        	case INS_SET_TERM_PUB_MOD:
        		// Set the terminal's public key
        		break;
        	case INS_SET_TERM_PUB_EXP:
        		// Set the terminal's public key
        		break;
			default:
				// good practice: If you don't know the INStruction, say so:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
        	break;
        case STATE_ISSUED:
        	// If the card has been finalized it is ready for regular use
        	switch (ins) {
        	case INS_MUT_AUTH:
        		handshake(apdu);
        		break;
			case INS_ADD_PTS:
				balance = add(apdu);
				break;
			case INS_SPEND_PTS:
				balance = spend(apdu);
				break;
			case INS_CHECK_BAL:
				balance = check(apdu);
				break;
			default:
				// good practice: If you don't know the INStruction, say so:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
        	break;
        default:
           ISOException.throwIt(SW_CONDITIONS_NOT_SATISFIED);
        }
		
		/* The last part of this function is the generation of a ResponseAPDU 
		 * Currently the code is incomplete and should be fixed. 
		 * */
		
		// Get the expected length of the ResponseAPDU
		le = apdu.setOutgoing();
		if (le < 5) {
            ISOException.throwIt((short) (SW_WRONG_LENGTH | 5));
        }
        
		/* 
		 * Somewhere in here we have to send the balance back to the terminal.
		 * I don't know how or when that data is being set however.
		 */
		
		// Set the INStruction byte (?) to 0 
        Util.setShort(buf, (short) 1, (short) 0);
        
        // Set the data to two bytes as given in value, starting at index 3
        Util.setShort(buf, (short) 3, value);
        
        // Set the actual length of the ResponseAPDU
        apdu.setOutgoingLength((short) 5);
        
        // Send the prepared ResponseAPDU
        // This method seems to allow multiple apdus sequentially
        apdu.sendBytes((short) 0, (short) 5);
	}
	
	/**
	 * After a lot of trial-and-error I found out that this function is responsible for successful selection.
	 * @return <code>true</code> <code>This</code> applet has been successfully selected. The status word will be <code>0x9000</code>.<br>
	 * <code>false</code> <code>This</code> applet has <i>not</i> been selected successfully. The status word will be <code>0x6999</code>.
	 */
	public boolean select () {
		/* Upon selecting this applet we want the card to authenticate itself to the terminal and vice versa,
		 * but this should be done via a CommandAPDU other than SELECT.
		 */
		return true;
	}
	
	/**
	 * Handles the processing of the data of the incoming APDU. First the data is retrieved from the data field of the APDU. 
	 * After that, the ResponseAPDU is prepared and ready to be filled in by the calling function.
	 * @param apdu The CommandAPDU to retrieve the data from.
	 * @return A blank ResponseAPDU to be filled in by the calling function. 
	 */
	private void handleAPDU(APDU apdu) {
		
		// Process the data
		short data = apdu.setIncomingAndReceive();
		short le = -1;
		byte[] ciphertext = new byte[2];
		
		// Convert the data from short to a two-byte array
		ciphertext[0] = (byte)(data & 0xff);
		ciphertext[1] = (byte)((data >> 8) & 0xff);
		
		// Decrypt the ciphertext
		byte[] plaintext = decrypt(ciphertext, skC);
		
		// TODO decide what to do with the data after decryption
		
		
		
		// Prepare for response		
		le = apdu.setOutgoing();
		
		// TODO add better response length check, 5 seems random
        if (le < 5) {
            ISOException.throwIt((short) (SW_WRONG_LENGTH | 5));
        }
        
        apdu.setOutgoingLength(le);		
	}

	/**
	 * Mutually authenticates this applet and the terminal using RSA 
	 * @param apdu The APDU containing the data for the handshake
	 * @return <code>true</code> if <code>this</code> applet was authenticated successfully 
	 */
	private void handshake(APDU apdu) {
		// TODO Implement mutual authentication algorithm using RSA
		//First create your name and a nonce
		byte[] name = {(byte) 0x00}; // Use the AID as name? Should be unique.
		byte[] nonce = {(byte) 0x00};
		byte[] challenge = {};
		// TODO Append these into a single challenge
		
		
		// Encrypt this challenge
		byte[] ciphertext = encrypt(challenge, pkT);
		
		// Send the challenge (no idea if this is correct)
		short respSize = apdu.setOutgoing();
		apdu.setOutgoingLength(respSize);
		apdu.sendBytesLong(ciphertext, (short) 0, respSize);
	}
	
	private byte[] encrypt(byte[] plaintext, Key key)  {
		byte[] ciphertext = null;
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key, random);
			ciphertext = cipher.doFinal(plaintext);
		} catch (InvalidKeyException e) {} 
		catch (IllegalBlockSizeException e) {}
		catch (BadPaddingException e) {}
		return ciphertext;
	}
	
	private byte[] decrypt(byte[] ciphertext, Key key) {
		byte[] plaintext = null;
		try {
			cipher.init(Cipher.DECRYPT_MODE, key);
			plaintext = cipher.doFinal(ciphertext);
		} catch (InvalidKeyException e1) {}
		catch (IllegalBlockSizeException e) {} 
		catch (BadPaddingException e) {}
		return plaintext;
	}
	
	private short add(APDU apdu) {
		// TODO Add credits to the current balance
		handleAPDU(apdu);
		short balance = 0;
		return balance;
	}
	
	private short spend(APDU apdu) {
		// TODO Subtract credits from the current balance, if sufficient
		handleAPDU(apdu);
		short balance = 0;
		return balance;
	}
	
	private short check(APDU apdu) {
		// TODO Report the current balance of the card
		handleAPDU(apdu);
		short balance = 0;
		return balance;
	}
}
