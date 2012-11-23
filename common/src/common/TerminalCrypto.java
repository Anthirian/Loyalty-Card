package common;

public class TerminalCrypto {

	private Cipher RSACipher;
	private Cipher AESCipher;
	private KeyGenerator AESKeyGen;
	private IvParameterSpec AESIvSpec;
	private Signature RSASign;
	
	public TerminalCrypto() {
		// initialize cipher classes
		try {
			RSACipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			AESCipher = Cipher.getInstance("AES/CBC/NoPadding");
			AESKeyGen = KeyGenerator.getInstance("AES");
			AESKeyGen.init(128);
			AESIvSpec = new IvParameterSpec(new byte[16]);
			RSASign = Signature.getInstance("SHA1withRSA");
		} catch (NoSuchAlgorithmException e) {
			// e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// e.printStackTrace();
		}
	}
}