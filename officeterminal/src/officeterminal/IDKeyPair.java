package officeterminal;

import java.security.KeyPair;

/**
 * Class to link a cardID to a KeyPair in a return value.
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 */

final class IDKeyPair {
	private int cardID;
	private KeyPair keys;

	IDKeyPair(int cardID, KeyPair keys) {
		this.cardID = cardID;
		this.keys = keys;
	}

	int getCardID() {
		return cardID;
	}

	KeyPair getKeys() {
		return keys;
	}
	
}
