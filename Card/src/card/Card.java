package card;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.KeyBuilder;
import javacard.security.RSAPrivateKey;
import javacard.security.RSAPublicKey;
import javacardx.crypto.Cipher;

/**
 * Java Card applet to be used for the Loyalty Card system
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class Card extends Applet implements ISO7816 {
	
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
	RSAPublicKey pkC;
	/** Secret Key of the Card. Used for decryption. */
	RSAPrivateKey skC;
	
	/** Cipher for encryption and decryption. */
	Cipher cipher;
	
	public Card () {
		tmp = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
		pkC = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);
		skC = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_512, false);
		cipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		state = STATE_INIT;
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
		
		/*
		 * The actual work happens here when a CommandAPDU is received by the card. 
		 */
		byte[] buf = apdu.getBuffer();
		short le = -1;
		short value = -1;
		byte ins = buf[ISO7816.OFFSET_INS];
		
		/*
		 * Decide what to do based on the instruction byte
		 * 
		 * (Incomplete) list of INS bytes as listed on http://www.cardwerk.com/smartcards/smartcard_standard_ISO7816-4_5_basic_organizations.aspx
		 * '0E' 	ERASE BINARY
		 * '20' 	VERIFY
		 * '70' 	MANAGE CHANNEL
		 * '82' 	EXTERNAL AUTHENTICATE
		 * '84' 	GET CHALLENGE
		 * '88' 	INTERNAL AUTHENTICATE
		 * 'A4' 	SELECT FILE
		 * 'B0' 	READ BINARY
		 * 'B2' 	READ RECORD(S)
		 * 'C0' 	GET RESPONSE
		 * 'C2' 	ENVELOPE
		 * 'CA' 	GET DATA
		 * 'D0' 	WRITE BINARY
		 * 'D2' 	WRITE RECORD
		 * 'D6' 	UPDATE BINARY
		 * 'DA' 	PUT DATA
		 * 'DC' 	UPDATE DATA
		 * 'E2' 	APPEND RECORD 
		 */
		switch(state) {
        case STATE_INIT:
        	switch (ins) {
			case (byte) 0x00:
				break;
			case '0':
				break;
			case '1':
				break;
			case '2':
				break;
			case '3':
				break;
			case '4':
				break;
			case '5':
				break;
			case '6':
				break;
			case '7':
				break;
			case '8':
				break;
			case '9':
				break;
			default:
				// good practice: If you don't know the INStruction, say so:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
        	break;
        case STATE_ISSUED:
        	switch (ins) {
			case (byte) 0x00:
				break;
			case '0':
				break;
			case '1':
				break;
			case '2':
				break;
			case '3':
				break;
			case '4':
				break;
			case '5':
				break;
			case '6':
				break;
			case '7':
				break;
			case '8':
				break;
			case '9':
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
        
		/* This line is used to indicate if the CalcApplet has a value in memory.
		 * It is not being used in the Card applet.
		 * 
		 * buf[0] = (m == 0) ? (byte) 0x00 : (byte) 0x01;
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
		// Upon selecting this applet we want the card to authenticate itself to the terminal and vice versa
		boolean auth_status = handshake();
		
		return auth_status;
	}

	/**
	 * Mutually authenticates this applet and the terminal using RSA 
	 * @return <code>true</code> if <code>this</code> applet was authenticated successfully 
	 */
	private boolean handshake() {
		// TODO Implement mutual authentication algorithm using RSA
		return true;
	}

}
