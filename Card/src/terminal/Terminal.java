package terminal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import common.KeyManager;

/**
 * The terminal class provides an interface for reading out the loyalty card system
 * @author Geert Smelt
 * @author Robin Oostrum
 *
 */

public class Terminal  {

	TerminalCrypto crypto;
	RSAPublicKey supermarketPublicKey;
	RSAPrivateKey privKey;
	String keyDir = "./keys/";
	
	int customerId;
	int cardId;
	int sequenceNr;
	
	public Terminal (int customerId) {
		this.customerId = customerId;
		System.out.println("Welcome customer no. " + customerId);
		loadKeyFiles();
		loadSequenceNumber();
	}
	
	private void loadKeyFiles() {
		try {
			String car = "Customer_" + this.customerId;
			privKey = (RSAPrivateKey) KeyManager.loadKeyPair(car).getPrivate();
			supermarketPublicKey = (RSAPublicKey) KeyManager.loadKeyPair("supermarket")
					.getPublic();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
