/**
 * 
 */
package card;

import javacard.framework.APDU;
import javacard.framework.APDUException;
import javacard.framework.ISO7816;
import javacard.framework.Applet;
import javacard.framework.ISOException;

/**
 * @author Geert Smelt (0609838)
 * @author Robin Oostrum (0609803)
 *
 */
public class Card extends Applet implements ISO7816 {
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new card.Card().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}

	public void process(APDU apdu) throws ISOException, APDUException {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		switch (buf[ISO7816.OFFSET_INS]) {
		case (byte) 0x00:
			break;
		default:
			// good practice: If you don't know the INStruction, say so:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
}