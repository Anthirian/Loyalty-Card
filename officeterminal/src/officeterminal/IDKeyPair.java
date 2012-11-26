package officeterminal;

import java.security.KeyPair;

/**
 * Class to link a cardID to a KeyPair in a return value.
 * 
 * @author Pol Van Aubel (paubel@science.ru.nl)
 * @author Marlon Baeten (mbaeten@science.ru.nl)
 * @author Sjors Gielen (sgielen@science.ru.nl)
 * @author Robert Kleinpenning (rkleinpe@science.ru.nl)
 * @author Jille Timmermans (jilletim@science.ru.nl)
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
