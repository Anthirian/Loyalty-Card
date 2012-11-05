package card;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 *
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class Card extends Applet implements ISO7816 {

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
		
		/*
		 * Decide what to do based on the instruction byte
		 */
		switch (buf[ISO7816.OFFSET_INS]) {
		case (byte) 0x00:
			break;
		case '0':
			//TODO Do something useful
			value = 42;
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
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
