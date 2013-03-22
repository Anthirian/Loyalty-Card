package common;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The class handling all the crypto in the terminal
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class TerminalCrypto {

	private Cipher RSACipher;
	private Cipher AESCipher;
	private KeyGenerator AESKeyGen;
	private IvParameterSpec AESIvSpec;
	private MessageDigest digest;
	
	public TerminalCrypto() {
		// initialize cipher classes
		try {
			RSACipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			AESCipher = Cipher.getInstance("AES/CBC/NoPadding");
			AESKeyGen = KeyGenerator.getInstance("AES");
			AESKeyGen.init(128);
			AESIvSpec = new IvParameterSpec(new byte[16]);
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Encrypt data with a specific public RSA key
	 * @param data data to be encrypted
	 * @param pubKey the public RSA key to perform encryption
	 * @return encrypted data
	 */
	public byte[] encrypt(byte[] data, RSAPublicKey pubKey) {
		SecretKey skey = AESKeyGen.generateKey();
		byte[] AESKey = skey.getEncoded();
		// encrypt the data array
		data = encryptAES(data, AESKey);
		// encrypt the key
		byte[] encryptedAESKey = encryptRSA(AESKey, pubKey);
		// append the 128 byte RSA encrypted AES key to the end of the ciphertext
		data = Arrays.copyOf(data, data.length + 128);
		System.arraycopy(encryptedAESKey, 0, data, data.length - 128, 128);
		return data;
	}
	
	/**
	 * Decrypt encrypted data with a specific private RSA key
	 * @param data encrypted data to be decrypted
	 * @param privKey the private RSA key to perform decryption
	 * @return decrypted data
	 */
	public byte[] decrypt(byte[] data, RSAPrivateKey privKey) {
		// extract the 128 byte RSA encrypted AES key from the end of the ciphertext
		byte[] encryptedAESKey = Arrays.copyOfRange(data, data.length - 128,
				data.length);
		data = Arrays.copyOfRange(data, 0, data.length - 128);
		// decrypt the key
		byte[] AESKey = decryptRSA(encryptedAESKey, privKey);
		// decrypt the data using AES
		data = decryptAES(data, AESKey);
		return data;
	}
	
	/**
	 * Generate a random nonce
	 * @param n 
	 * @return a random nonce
	 */
	public byte[] generateRandomNonce(int n) {
		SecureRandom random = new SecureRandom();
		byte[] nonce = new byte[n];
		random.nextBytes(nonce);
		return nonce;
	}
	
	/**
	 * Encrypt data with a given public RSA key
	 * @param data the data to be encrypted
	 * @param pubkey the key to perform encryption
	 * @return encrypted data
	 */
	public byte[] encryptRSA(byte[] data, RSAPublicKey pubKey) {
		try {
			// perform rsa encryption
			RSACipher.init(Cipher.ENCRYPT_MODE, pubKey);
			return RSACipher.doFinal(data);
			// cipherData is 128 bytes long and is appended to the data array
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Decrypt encrypted data with a given private RSA key
	 * @param data the data to be decrypted
	 * @param privkey the private key to perform decryption
	 * @return decrypted data
	 */	
	public byte[] decryptRSA(byte[] data, RSAPrivateKey privKey) {
		try {
			// perform rsa decryption
			RSACipher.init(Cipher.DECRYPT_MODE, privKey);
			return RSACipher.doFinal(data);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Decrypt encrypted data with a given AES key
	 * @param data the data to be decrypted
	 * @param AESKey the key to perform decryption
	 * @return decrypted data
	 */
	public byte[] decryptAES(byte[] data, byte[] AESKey) {
		try {
			SecretKeySpec AESKeySpec = new SecretKeySpec(AESKey, "AES");
			// perform decryption
			AESCipher.init(Cipher.DECRYPT_MODE, AESKeySpec, AESIvSpec);
			data = AESCipher.doFinal(data);
			return stripPaddingAES(data);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return null;
		} catch (BadPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Encrypt data with a given AES key
	 * @param data the data to be encrypted
	 * @param AESKey the key to perform encryption
	 * @return encrypted data
	 */
	public byte[] encryptAES(byte[] data, byte[] AESKey) {
		try {
			// pad data before encrypting with AES
			data = padAES(data);
			SecretKeySpec AESKeySpec = new SecretKeySpec(AESKey, "AES");
			// perform encryption
			AESCipher.init(Cipher.ENCRYPT_MODE, AESKeySpec, AESIvSpec);
			return AESCipher.doFinal(data);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return null;
		} catch (BadPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Pad plaintext data to be encrypted with AES
	 * @param data the data to be padded
	 * @return padded data with length of multiple of block size
	 */
	private byte[] padAES(byte[] data) {
		int cipherLen = data.length + 2;
		cipherLen += (16 - (cipherLen % 16));
		byte[] newData = new byte[cipherLen];
		System.arraycopy(Formatter.toByteArray((short) data.length), 0,
				newData, 0, 2);
		System.arraycopy(data, 0, newData, 2, data.length);
		return newData;
	}
	
	/**
	 * Strip padding from the AES-decrypted data
	 * @param data the decrypted data with padding bytes
	 * @return the decrypted data without padding bytes
	 */
	private byte[] stripPaddingAES(byte[] data) {
		byte[] pad = Arrays.copyOfRange(data, 0, 2);
		int padding = Formatter.byteArrayToShort(pad);
		return Arrays.copyOfRange(data, 2, 2 + padding);
	}
	
	/**
	 * Hashes the input data
	 * @param data the input data
	 * @return a 20 byte MAC of the input data
	 */
	public byte[] hash(byte[] data) {
		digest.update(data);
		return digest.digest();
	}
	
	/*
	//for debugging: print byte buffer as hexadecimal
	private static void printHex(String msg, byte buf[]) {
		StringBuffer strbuf = new StringBuffer(buf.length * 2);
		for (int i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10) {
				strbuf.append("0");
			}
			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}
		System.out.println(msg + ": " + strbuf.toString());
	}
	*/
	
	// main function for debugging purposes only!
	// testing encryption, signing, verification and decryption
	public static void main(String[] arg) {
		try {
			// generate keys
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(512);
			//KeyPair keypair;
			
			TerminalCrypto pk = new TerminalCrypto();
			byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
					12, 13, 14, 15, 16 };
			
			System.out.println(Arrays.toString(data));
			System.out.println(Arrays.toString(pk.hash(data)));
				
			
			/*
			try {
				keypair = KeyManager.loadKeyPair("/home/javacard/workspace/Loyalty-Card/officeterminal/keys/","supermarket");
				RSAPublicKey pubKey = (RSAPublicKey) keypair.getPublic();
				RSAPrivateKey privKey = (RSAPrivateKey) keypair.getPrivate();
				
				BigInteger Exponent = pubKey.getPublicExponent();
				BigInteger Modulus = pubKey.getModulus();
				byte[] testExp = test.getExponent();
				byte[] testMod = test.getModulus();
				
				System.out.println("Exponent: \n" + Exponent);
				BigInteger testExpInt = new BigInteger(testExp);
				System.out.println(testExpInt);
				
				System.out.println("Modulus: \n" + Modulus);			
				BigInteger testModInt = new BigInteger(testMod);
				System.out.println(testModInt);
				
				byte[] array = Modulus.toByteArray();
				if (array[0] == 0) {
				    byte[] tmp = new byte[array.length - 1];
				    System.arraycopy(array, 1, tmp, 0, tmp.length);
				    array = tmp;
				}
				
				BigInteger testtest = new BigInteger (array);
				System.out.println(testtest);
				
				
				// perform encryption and decryption with test data
				
				TerminalCrypto pk = new TerminalCrypto();
				byte[] data = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
						12, 13, 14, 15, 16 };
				printHex("Cleartext", data);
				data = pk.encryptRSA(data, pubKey);
				printHex("After encryption", data);
				
				data = pk.decryptRSA(data, privKey);
				printHex("After decryption", data);
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			}
			*/
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

}