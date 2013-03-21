package common;

/**
 * Class featuring all our static definitions and constants used in other classes
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class CONSTANTS {
	// TODO Ensure only used constants remain.
	public static final int KEY_SIZE = 512;
	// public static final int SEQNR_BYTESIZE = 4;
	public static final byte STATE_INIT = 0;
	public static final byte STATE_ISSUED = 1;
	public static final byte STATE_REVOKED = 2;

	public static final byte[] NAME_TERM = {(byte) 0x54, (byte) 0x45, (byte) 0x52, (byte) 0x4d}; // Hex for "TERM"
	public static final byte[] NAME_CARD = {(byte) 0x43, (byte) 0x41, (byte) 0x52, (byte) 0x44}; // Hex for "CARD"
	public static final short NAME_LENGTH = (short) 4;

	public static final byte CRYPTO_TYPE_SYMMETRIC = (byte) 0xC8;
	public static final byte CRYPTO_TYPE_ASYMMETRIC = (byte) 0xC9;

	/* APDU chaining APDUs */
	public static final byte CLA_CHAIN_LAST_OR_NONE = (byte) 0x00;
	public static final byte CLA_CHAIN_FIRST_OR_NEXT = (byte) 0x10;
	public static final byte CLA_DEF = (byte) 0x01;

	/* Personalization APDUs */
	public static final byte INS_PERSONALIZE_WRITE = (byte) 0x07;

	/* Authentication APDUs */
	public static final byte INS_AUTHENTICATE = (byte) 0x09;
	public static final byte P1_AUTHENTICATE_OFFICE = (byte) 0x01;
	public static final byte P1_AUTHENTICATE_SUPERMARKET = (byte) 0x02;
	public static final byte P1_AUTHENTICATE_CARD = (byte) 0x03;
	public static final byte P2_AUTHENTICATE_STEP1 = (byte) 0x01;
	public static final byte P2_AUTHENTICATE_STEP2 = (byte) 0x02;

	/* Card issuance APDUs */
	public static final byte INS_REVOKE = (byte) 0x0A;
	public static final byte INS_GET_PUBKEY = (byte) 0x0E;

	/* Balance operations instructions */
	public static final byte INS_BAL_INC = (byte) 0x0B;
	public static final byte INS_BAL_CHECK = (byte) 0x0C;
	public static final byte INS_BAL_DEC = (byte) 0x0D;

	public static final byte INS_MORE_DATA = (byte) 0x0F;

	/* Indicators for session establishment */
	public static final byte SESSION_ESTABLISHED = (byte) 0xCC;
	public static final byte NO_ACTIVE_SESSION = (byte) 0xDD;

	/* Success codes, as defined by ISO 7816, 5.1.3 */
	public static final byte SW1_SUCCESS = (byte) 0x90;

	// Our self defined code to indicate further response data
	// Use: ISOException.throwIt(((SW1_MORE_DATA << 8) & 0xff00) | SW2_MORE_DATA);
	public static final byte SW1_MORE_DATA = (byte) 0x67;
	public static final byte SW2_MORE_DATA = (byte) 0x01;

	/* Warning processing */
	public static final byte SW1_NON_VOLATILE_UNCHANGED_WARN_00 = (byte) 0x62;
	public static final byte SW1_NON_VOLATILE_CHANGED_WARN_00 = (byte) 0x63;

	/* Execution errors */
	public static final byte SW1_NON_VOLATILE_UNCHANGED_ERROR_00 = (byte) 0x64;
	public static final byte SW1_NON_VOLATILE_CHANGED_ERROR_00 = (byte) 0x65;
	public static final byte SW1_SECURITY_RELATED_ISSUE_00 = (byte) 0x66;

	/* Checking errors */
	public static final byte SW1_WRONG_LENGTH = (byte) 0x67;
	public static final byte SW1_FUNCTION_NOT_SUPPORTED_00 = (byte) 0x68;
	public static final byte SW1_COMMAND_NOT_ALLOWED_00 = (byte) 0x69;
	public static final byte SW1_WRONG_PARAMETERS_00 = (byte) 0x6A;
	public static final byte SW1_WRONG_PARAMETERS = (byte) 0x6B;
	public static final byte SW1_WRONG_LE_FIELD_00 = (byte) 0x6C;
	public static final byte SW1_INS_NOT_SUPPORTED = (byte) 0x6D;
	public static final byte SW1_CLASS_NOT_SUPPORTED = (byte) 0x6E;
	public static final byte SW1_NO_PRECISE_DIAGNOSIS = (byte) 0x6F;

	/* Self-Defined */
	public static final byte SW1_AUTH_EXCEPTION = (byte) 0xAE;
	public static final byte SW1_CRYPTO_EXCEPTION = (byte) 0xCE;
	public static final byte SW1_PERS_EXCEPTION = (byte) 0xDE;

	/* Data transfer issues */
	public static final byte SW2_LC_INCORRECT = (byte) 0x10;
	public static final byte SW2_CHAINING_WRONG_INS = (byte) 0x11;
	public static final byte SW2_RESP_NO_CHUNK_TO_SEND = (byte) 0x12;
	public static final byte SW2_RESP_CHAING_WRONG_INS = (byte) 0x13;
	public static final byte SW2_RESP_CHAING_WRONG_LEN = (byte) 0x14;
	public static final byte SW2_WRONG_INS = (byte) 0x15;
	public static final byte SW2_READ_TOO_SHORT = (byte) 0x16;

	/* Writing credits data issues */
	public static final byte SW2_CREDITS_WRONG_LENGTH = (byte) 0xE0;
	public static final byte SW2_CREDITS_INSUFFICIENT = (byte) 0xE1;
	public static final byte SW2_CREDITS_NEGATIVE = (byte) 0xE2;
	public static final byte SW2_CREDITS_TOO_MANY = (byte) 0xE3;

	/* Security related issues */
	public static final byte SW2_AUTH_OTHER_ERROR = (byte) 0xA0;
	public static final byte SW2_AUTH_STEP_INCORRECT = (byte) 0xA1;
	public static final byte SW2_AUTH_WRONG_NONCE = (byte) 0xA2;
	public static final byte SW2_AUTH_WRONG_PARTNER = (byte) 0xA4;
	public static final byte SW2_AUTH_WRONG_2 = (byte) 0xA5;
	public static final byte SW2_AUTH_PARTNER_KEY_NOT_INIT = (byte) 0xA6;
	public static final byte SW2_NO_AUTH_PERFORMED = (byte) 0xA7;
	public static final byte SW2_AUTH_ALREADY_PERFORMED = (byte) 0xA8;
	public static final byte SW2_AUTH_INCORRECT_MESSAGE_LENGTH = (byte) 0xA9;
	public static final byte SW2_AUTH_CARD_KEY_NOT_INIT = (byte) 0xAA;

	public static final byte SW2_SESSION_ENCRYPT_ERR = (byte) 0xC2;
	public static final byte SW2_CIPHERTEXT_NOT_ALIGNED = (byte) 0xC5;
	public static final byte SW2_UNSUPPORTED_CRYPTO_MODE = (byte) 0xC6;

	/* Personalization issues */
	public static final byte SW2_ALREADY_ISSUED = (byte) 0xB0;
	public static final byte SW2_CARD_REVOKED = (byte) 0xB1;

	/* Internal issues */
	public static final byte SW2_INTERNAL_ERROR = (byte) 0x6F;

	/* Sizes and offsets */
	public static final short APDU_SIZE_MAX = (short) 255;
	public static final short APDU_DATA_SIZE_MAX = (short) 236;
	public static final short APDU_MESSAGE_CRYPTO_OVERHEAD = (short) 3;
	public static final short APDU_MESSAGE_SIZE_MAX = APDU_DATA_SIZE_MAX + APDU_MESSAGE_CRYPTO_OVERHEAD;
	// TODO Change DATA_SIZE_MAX to 512?
	public static final short DATA_SIZE_MAX = (short) 1024;

	public static final short NONCE_LENGTH = (short) 8;
	public static final short ID_LENGTH = (short) 4;
	public static final short SEQ_LENGTH = (short) 4;
	public static final short DATE_LENGTH = (short) 4;
	public static final short CREDITS_LENGTH = (short) 2;
	public static final short CREDITS_MAX = (short) 32767;

	// Only when using RSA 512 bit and AES 128 bit, obviously.
	public static final short AES_IV_LENGTH = (short) 16;
	public static final short AES_KEY_LENGTH = (short) 16;
	public static final short RSA_KEY_MOD_LENGTH = (short) 64;
	public static final short RSA_KEY_PUBEXP_LENGTH = (short) 3;
	public static final short RSA_KEY_PRIVEXP_LENGTH = (short) 64;

	// RSA key buffer
	// Builds the following buffer: [ ID | MODULUS | EXPONENT ]
	public static final short RSA_PUBKEY_OFFSET_ID = (short) 0;
	public static final short RSA_PUBKEY_OFFSET_MOD = (short) (RSA_PUBKEY_OFFSET_ID + ID_LENGTH);
	public static final short RSA_PUBKEY_OFFSET_EXP = (short) (RSA_PUBKEY_OFFSET_MOD + RSA_KEY_MOD_LENGTH);
	public static final short RSA_PUBKEY_LENGTH = (short) (RSA_PUBKEY_OFFSET_EXP + RSA_KEY_PUBEXP_LENGTH);

	// The first message of the handshake
	// Builds the following buffer: [ T ]
	// Sent from the Terminal to the Card
	public static final short AUTH_MSG_1_OFFSET_NAME_TERM = (short) 0;
	public static final short AUTH_MSG_1_TOTAL_LENGTH = (short) (AUTH_MSG_1_OFFSET_NAME_TERM + NAME_LENGTH);

	// The second message of the handshake
	// Builds the following buffer: [ C | T | NC ]
	// Sent from the Card to the Terminal
	public static final short AUTH_MSG_2_OFFSET_NAME_CARD = (short) 0;
	public static final short AUTH_MSG_2_OFFSET_NAME_TERM = (short) (AUTH_MSG_2_OFFSET_NAME_CARD + NAME_LENGTH);
	public static final short AUTH_MSG_2_OFFSET_NC = (short) (AUTH_MSG_2_OFFSET_NAME_TERM + NAME_LENGTH);
	public static final short AUTH_MSG_2_TOTAL_LENGTH = (short) (AUTH_MSG_2_OFFSET_NC + NONCE_LENGTH);

	// The third message of the handshake
	// Builds the following buffer: [ T | C | N_C | N_T ]
	// Sent from the Terminal to the Card
	public static final short AUTH_MSG_3_OFFSET_NAME_TERM = (short) 0;
	public static final short AUTH_MSG_3_OFFSET_NAME_CARD = (short) (AUTH_MSG_3_OFFSET_NAME_TERM + NAME_LENGTH);
	public static final short AUTH_MSG_3_OFFSET_NC = (short) (AUTH_MSG_3_OFFSET_NAME_CARD + NONCE_LENGTH);
	public static final short AUTH_MSG_3_OFFSET_NT = (short) (AUTH_MSG_3_OFFSET_NC + NONCE_LENGTH);
	public static final short AUTH_MSG_3_TOTAL_LENGTH = (short) (AUTH_MSG_3_OFFSET_NT + NONCE_LENGTH);

	// The fourth message of the handshake
	// Builds the following buffer: [ C | T | N_T | k ]
	// Sent from the Card to the Terminal
	public static final short AUTH_MSG_4_OFFSET_NAME_CARD = (short) 0;
	public static final short AUTH_MSG_4_OFFSET_NAME_TERM = (short) (AUTH_MSG_4_OFFSET_NAME_CARD + NAME_LENGTH);
	public static final short AUTH_MSG_4_OFFSET_NT = (short) (AUTH_MSG_4_OFFSET_NAME_TERM + NAME_LENGTH);
	public static final short AUTH_MSG_4_OFFSET_SESSION_KEY = (short) (AUTH_MSG_4_OFFSET_NT + NONCE_LENGTH);
	public static final short AUTH_MSG_4_TOTAL_LENGTH = (short) (AUTH_MSG_4_OFFSET_SESSION_KEY + AES_KEY_LENGTH);

	public static final short PUB_KEY_CARD_EXP_OFF = (short) 0;
	public static final short PUB_KEY_CARD_MOD_OFF = (short) (PUB_KEY_CARD_EXP_OFF + RSA_KEY_PUBEXP_LENGTH);
}

