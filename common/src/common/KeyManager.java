package common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * The KeyManager creates and stores public and private keys
 * @author Geert Smelt
 * @author Robin Oostrum
 *
 */
public class KeyManager {
	private String path;
	public static final String PUBKEY_BASENAME = "pubkey_rsa.";
	public static final String PRIVKEY_BASENAME = "privkey_rsa.";

	public KeyManager() {
		this.path = "./keys/";
	}
	
	public KeyManager(String path) {
		this.path = path + '/';
	}
	
	public KeyPair loadKeys(String identifier) throws NoSuchAlgorithmException,
			InvalidKeySpecException, FileNotFoundException, IOException {
		KeyFactory factory = KeyFactory.getInstance("RSA");
		
		String publicPath = path + PUBKEY_BASENAME + identifier;
		String privatePath = path + PRIVKEY_BASENAME + identifier;
		
		//standard way of encoding public keys using X509EncodedKeySpec
		X509EncodedKeySpec publicKeyEncoded = new X509EncodedKeySpec(
				loadKey(publicPath));
	
		//standard way of encoding private keys using PKCS8EncodedKeySpec
		PKCS8EncodedKeySpec privateKeyEncoded = new PKCS8EncodedKeySpec(
				loadKey(privatePath));
		
		RSAPublicKey publicKey = (RSAPublicKey) factory
				.generatePublic(publicKeyEncoded);
		RSAPrivateKey privateKey = (RSAPrivateKey) factory
				.generatePrivate(privateKeyEncoded);
		return new KeyPair(publicKey, privateKey);
	}
	
	private byte[] loadKey(String path) throws FileNotFoundException,
			IOException {
		FileInputStream file = new FileInputStream(path);
		byte[] bytes = new byte[file.available()];
		file.read(bytes);
		file.close();
		return bytes;
	}
	
	public KeyPair generateAndSave(String identifier)
			throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		KeyPair pair = KeyManager.generate();
		save(pair, identifier);
		return pair;
	}
	
	public void save(KeyPair keypair, String identifier)
			throws FileNotFoundException, IOException {
		FileOutputStream file = new FileOutputStream(path + PUBKEY_BASENAME
				+ identifier);
		file.write(keypair.getPublic().getEncoded());
		file.close();
		
		file = new FileOutputStream(path + PRIVKEY_BASENAME + identifier);
		file.write(keypair.getPrivate().getEncoded());
		file.close();
	}
	
	public static KeyPair loadKeyPair(String path, String identifier)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			FileNotFoundException, IOException {
		KeyManager m = new KeyManager(path);
		return m.loadKeys(identifier);
	}

	public static KeyPair loadKeyPair(String identifier)
			throws NoSuchAlgorithmException, InvalidKeySpecException,
			FileNotFoundException, IOException {
		KeyManager m = new KeyManager();
		return m.loadKeys(identifier);
	}
	
	public static KeyPair generateAndSave(String path, String identifier)
			throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		KeyManager m = new KeyManager(path);
		return m.generateAndSave(identifier);
	}
	
	public static KeyPair generate() throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(CONSTANTS.KEY_SIZE);
		return generator.generateKeyPair();
	}
}