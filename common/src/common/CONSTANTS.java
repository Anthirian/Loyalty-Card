package common;

/**
 * Class featuring all our static definitions and constants
 * used in other classes
 * @author Geert Smelt
 * @author Robin Oostrum
 */
public class CONSTANTS {
	public static final int KEY_SIZE = 512;
	public static final int SEQNR_BYTESIZE = 4;
	public static final byte STATE_INIT = 0;
	public static final byte STATE_ISSUED = 1;

	public static final byte NAME_TERMINAL = (byte) 0x84; // randomly chosen
	public static final byte NAME_CARD = (byte) 0x29; // randomly chosen
	public static final short NAME_LENGTH = (short) 1; 
	
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
	public static final byte INS_ISSUE = (byte) 0x0A;

	/* Balance operations instructions */
	public static final byte INS_BAL_INC = (byte) 0x0B;
	public static final byte INS_BAL_CHECK = (byte) 0x0C;
	public static final byte INS_BAL_DEC = (byte) 0x0D;

	/* Couldn't get the ISO standard APDU to work, so put this in. */
	public static final byte INS_GET_RESPONSE = (byte) 0x0F;

	public static final byte INS_TEST_TEST_TEST = (byte) 0x04;

	/* Indicator for session establishment */
	public static final byte SESSION_ESTABLISHED = (byte) 0xCC;

	/* Success codes, as defined by ISO 7816, 5.1.3 */
	public static final byte SW1_SUCCESS = (byte) 0x90;
	public static final byte SW1_MORE_DATA_AVAILABLE_00 = (byte) 0x61; // DO NOT USE
	/*
	 * Our self defined code to indicate further response data Use: ISOException.throwIt(((SW1_MORE_DATA << 8) & 0xff00) | SW2_MORE_DATA);
	 */
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

	/* Security related issues */
	public static final byte SW2_AUTH_OTHER_ERROR = (byte) 0xA0;
	public static final byte SW2_AUTH_STEP_INCORRECT = (byte) 0xA1;
	public static final byte SW2_AUTH_WRONG_NONCE = (byte) 0xA2;
	public static final byte SW2_AUTH_WRONG_CARID = (byte) 0xA3;
	public static final byte SW2_AUTH_WRONG_PARTNER = (byte) 0xA4;
	public static final byte SW2_AUTH_WRONG_2 = (byte) 0xA5;
	public static final byte SW2_AUTH_PARTNER_KEY_NOT_INIT = (byte) 0xA6;
	public static final byte SW2_NO_AUTH_PERFORMED = (byte) 0xA7;
	public static final byte SW2_AUTH_ALREADY_PERFORMED = (byte) 0xA8;
	public static final byte SW2_AUTH_INCORRECT_MESSAGE_LENGTH = (byte) 0xA9;

	public static final byte SW2_SESSION_BROKEN = (byte) 0xC0;
	public static final byte SW2_SESSION_WRONG_NONCE = (byte) 0xC1;
	public static final byte SW2_SESSION_ENCRYPT_ERR = (byte) 0xC2;
	public static final byte SW2_SESSION_WRONG_CTR = (byte) 0xC3;
	public static final byte SW2_SESSION_WRONG_SIG = (byte) 0xC4;

	public static final byte SW2_CIPHERTEXT_NOT_ALIGNED = (byte) 0xC5;

	public static final byte SW2_MSG_CTR_OVERFLOW = (byte) 0xC6;

	/* Personalization issues */
	// public static final byte SW2_PERS_ALREADY_DONE = (byte) 0xB0;
	// public static final byte SW2_PERS_INCORRECT_LEN = (byte) 0xB1;
	// public static final byte SW2_PERS_INVALID_SIG = (byte) 0xB2; 
	// public static final byte SW2_PERS_INVALID_PUBSIG = (byte) 0xB3; 
	// public static final byte SW2_PERS_NOT_PERSONALIZED = (byte) 0xB4; 
	// public static final byte SW2_PERS_SUCCESS = (byte) 0xBB;
	
	/* Renting issues */
	public static final byte SW2_RENT_WRONG_LENGTH = (byte) 0xD0;
	public static final byte SW2_RENT_ALREADY_RENTED = (byte) 0xD1;

	/* Internal issues */
	public static final byte SW2_INTERNAL_ERROR = (byte) 0x6F;

	/* Sizes and offsets */
	public static final short APDU_SIZE_MAX = (short) 255;
	public static final short APDU_DATA_SIZE_MAX = (short) 236;
	public static final short APDU_MESSAGE_CRYPTO_OVERHEAD = (short) 3;
	public static final short APDU_MESSAGE_SIZE_MAX = APDU_DATA_SIZE_MAX + APDU_MESSAGE_CRYPTO_OVERHEAD;
	// TODO Change to 512?
	public static final short DATA_SIZE_MAX = (short) 1024;

	public static final short NONCE_LENGTH = (short) 8;
	public static final short ID_LENGTH = (short) 4;
	public static final short SEQ_LENGTH = (short) 4;
	public static final short DATE_LENGTH = (short) 4;
	public static final short CREDITS_LENGTH = (short) 4;

	// Only when using RSA 1024 bit and AES 128 bit, obviously.
	public static final short AES_IV_LENGTH = (short) 16;
	public static final short AES_KEY_LENGTH = (short) 16;
	public static final short RSA_KEY_MOD_LENGTH = (short) 128;
	public static final short RSA_KEY_PUBEXP_LENGTH = (short) 16;
	public static final short RSA_KEY_PRIVEXP_LENGTH = (short) 128;
	public static final short RSA_KEY_CRT_COMP_LENGTH = (short) 64;
	public static final short RSA_SIGNATURE_LENGTH = (short) 128;

	public static final short RSA_SIGNED_PUBKEY_OFFSET_ID = (short) 0;
	public static final short RSA_SIGNED_PUBKEY_OFFSET_MOD = (short) (RSA_SIGNED_PUBKEY_OFFSET_ID + ID_LENGTH);
	public static final short RSA_SIGNED_PUBKEY_OFFSET_EXP = (short) (RSA_SIGNED_PUBKEY_OFFSET_MOD + RSA_KEY_MOD_LENGTH);
	public static final short RSA_SIGNED_PUBKEY_OFFSET_SIG = (short) (RSA_SIGNED_PUBKEY_OFFSET_EXP + RSA_KEY_PUBEXP_LENGTH);
	public static final short RSA_SIGNED_PUBKEY_LENGTH = (short) (RSA_SIGNED_PUBKEY_OFFSET_SIG + RSA_SIGNATURE_LENGTH);

	// public static final short PERS_MSG_OFFSET_PRIVEXP = (short)
	// (PERS_MSG_OFFSET_CARDID + ID_LENGTH);
	public static final short PERS_MSG_OFFSET_PRIV_P = (short) 0;
	public static final short PERS_MSG_OFFSET_PRIV_Q = (short) (PERS_MSG_OFFSET_PRIV_P + RSA_KEY_CRT_COMP_LENGTH);
	public static final short PERS_MSG_OFFSET_PRIV_DP = (short) (PERS_MSG_OFFSET_PRIV_Q + RSA_KEY_CRT_COMP_LENGTH);
	public static final short PERS_MSG_OFFSET_PRIV_DQ = (short) (PERS_MSG_OFFSET_PRIV_DP + RSA_KEY_CRT_COMP_LENGTH);
	public static final short PERS_MSG_OFFSET_PRIV_PQ = (short) (PERS_MSG_OFFSET_PRIV_DQ + RSA_KEY_CRT_COMP_LENGTH);
	// public static final short PERS_MSG_OFFSET_MOD = (short)
	// (PERS_MSG_OFFSET_PRIV_PQ + RSA_KEY_CRT_COMP_LENGTH);
	public static final short PERS_MSG_OFFSET_PUBKEY = (short) (PERS_MSG_OFFSET_PRIV_PQ + RSA_KEY_CRT_COMP_LENGTH);
	// The above is NOT a bug, modulus is read twice from same location.
	public static final short PERS_MSG_LENGTH = (short) (PERS_MSG_OFFSET_PUBKEY + RSA_SIGNED_PUBKEY_LENGTH);

	public static final short CERT_OFFSET_CARDID = (short) 0;
	public static final short CERT_OFFSET_CARID = (short) (CERT_OFFSET_CARDID + ID_LENGTH);
	public static final short CERT_OFFSET_SEQNUM = (short) (CERT_OFFSET_CARID + ID_LENGTH);
	public static final short CERT_OFFSET_START = (short) (CERT_OFFSET_SEQNUM + SEQ_LENGTH);
	public static final short CERT_OFFSET_END = (short) (CERT_OFFSET_START + DATE_LENGTH);
	public static final short CERT_OFFSET_SIG = (short) (CERT_OFFSET_END + DATE_LENGTH);
	public static final short CERT_PLAIN_LENGTH = (short) CERT_OFFSET_SIG;
	public static final short CERT_LENGTH = (short) (CERT_OFFSET_SIG + RSA_SIGNATURE_LENGTH);

	public static final short RENT_MSG_OFFSET_PUB_EXP = (short) 0;
	public static final short RENT_MSG_OFFSET_PUB_MOD = (short) (RENT_MSG_OFFSET_PUB_EXP + RSA_KEY_PUBEXP_LENGTH);
	public static final short RENT_MSG_OFFSET_CERT = (short) (RENT_MSG_OFFSET_PUB_MOD + RSA_KEY_MOD_LENGTH);
	public static final short RENT_MSG_LENGTH = (short) (RENT_MSG_OFFSET_CERT + CERT_LENGTH);

	/* Their first message of the handshake */
	public static final short AUTH_MSG_1_OFFSET_NA = (short) 0;
	public static final short AUTH_MSG_1_OFFSET_ID = (short) (AUTH_MSG_1_OFFSET_NA + NONCE_LENGTH);
	public static final short AUTH_MSG_1_OFFSET_SIGNED_PUBKEY = (short) (AUTH_MSG_1_OFFSET_NA + NONCE_LENGTH); // Not a bug.
	public static final short AUTH_MSG_1_LENGTH = (short) (AUTH_MSG_1_OFFSET_SIGNED_PUBKEY + RSA_SIGNED_PUBKEY_LENGTH);

	/* Our first message of the handshake */
	public static final short AUTH_MSG_1_OFFSET_PARTNER_NAME = (short) 0;
	public static final short AUTH_MSG_1_DATA_SIZE = (short) 1;
	public static final short AUTH_MSG_1_TOTAL_LENGTH = (short) (AUTH_MSG_1_OFFSET_PARTNER_NAME + AUTH_MSG_1_DATA_SIZE);
	
	/* Second message of the handshake */
	public static final short AUTH_MSG_2_OFFSET_NA = (short) 0;
	public static final short AUTH_MSG_2_OFFSET_NB = (short) (AUTH_MSG_2_OFFSET_NA + NONCE_LENGTH);
	public static final short AUTH_MSG_2_OFFSET_ID = (short) (AUTH_MSG_2_OFFSET_NB + NONCE_LENGTH);
	public static final short AUTH_MSG_2_LENGTH = (short) (AUTH_MSG_2_OFFSET_ID + ID_LENGTH);
	public static final short AUTH_MSG_2_OFFSET_NAME_CARD = (short) 0;
	public static final short AUTH_MSG_2_OFFSET_NAME_TERM = AUTH_MSG_2_OFFSET_NAME_CARD + NAME_LENGTH;
	
	/* Third message of the handshake */
	public static final short AUTH_MSG_3_OFFSET_NB = (short) 0;
	public static final short AUTH_MSG_3_OFFSET_CERT = (short) (AUTH_MSG_3_OFFSET_NB + NONCE_LENGTH);
	public static final short AUTH_MSG_3_LENGTH = (short) (AUTH_MSG_3_OFFSET_CERT + CERT_LENGTH);

	public static final short CREDITS_MSG_OFFSET_VAL = (short) 0;
	public static final short CREDITS_MSG_OFFSET_SIG = (short) (CREDITS_MSG_OFFSET_VAL + CREDITS_LENGTH);
	public static final short CREDITS_MSG_LENGTH = (short) (CREDITS_MSG_OFFSET_SIG + RSA_SIGNATURE_LENGTH);
	
}