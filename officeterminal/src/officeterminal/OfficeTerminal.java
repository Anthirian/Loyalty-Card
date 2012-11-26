package officeterminal;

import common.AppletSession;
import common.CLI;
import common.CONSTANTS;
import common.Formatter;
import common.Response;
import common.AppletCommunication;
import common.TerminalCrypto;
import common.KeyManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.CardChannel;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class OfficeTerminal {
	static final int BLOCKSIZE = 128;

	static final String MSG_ERROR = "Error";
	static final String MSG_INVALID = "Invalid";

	static final byte CLA_ISO = (byte) 0x00;
	static final byte INS_SELECT = (byte) 0xA4;
	static final short SW_NO_ERROR = (short) 0x9000;
	static final short SW_APPLET_SELECT_FAILED = (short) 0x6999;
	static final short SW_FILE_NOT_FOUND = (short) 0x6A82;

	/** The card applet. */
	CardChannel applet;

	/** communication gateway */
	AppletCommunication com;

	/** communication session */
	AppletSession session;

	/** Back-office for OfficeTerminal */
	BackOffice office;

	/**
	 * Constructs the terminal application.
	 */
	public OfficeTerminal() {
		try {
			office = new BackOfficeSimulator("./keys/");
		} catch (BackOfficeException e) {
			System.err.println("Failed to create back office simulator: "
					+ e.getMessage());
			return;
		}

		try {
			RSAPrivateKey supermarketPrivateKey = (RSAPrivateKey) office
					.getSupermarketKeyPair().getPrivate();
			RSAPublicKey supermarketPublicKey = (RSAPublicKey) office
					.getSupermarketKeyPair().getPublic();
			session = new AppletSession(supermarketPublicKey, supermarketPrivateKey, 0);
		} catch (BackOfficeException e) {
			System.err.println("Failed to fetch supermarket private key: "
					+ e.getMessage());
		}
		com = new AppletCommunication(session);
	}

	public Customer getCustomer() {
		try {
			return office.getCustomerByCard(session.getCardId());
		} catch (BackOfficeException e) {
			// System.err.println("Cannot get customer. Card not authenticated.");
			return null;
		}
	}

	/**
	 * Save the back-office state.
	 */
	public void save() {
		BackOfficeSimulator sim = (BackOfficeSimulator) office;
		try {
			sim.save();
		} catch (IOException e) {
			System.err.println("Failed to save BackOfficeSimulator state: "
					+ e.getMessage());
		}
	}

	/**
	 * Personalize the inserted card for the given client ID.
	 */
	public void personalizeCard(int clientID) {
		Customer client;
		try {
			client = office.getCustomerByID(clientID);
		} catch (BackOfficeException e) {
			System.err.println("Failed loading client information:"
					+ e.getMessage());
			return;
		}

		com.waitForCard();

		// Generate a card keypair, upload the private key to the card, and save
		// the public key.
		IDKeyPair cardKeyPair;
		try {
			cardKeyPair = office.issueNewCard(client);
		} catch (BackOfficeException e) {
			System.err.println("Error issuing new card: " + e.getMessage());
			return;
		}

		RSAPublicKey customerPublicKey = (RSAPublicKey) cardKeyPair.getKeys()
				.getPublic();
		RSAPrivateCrtKey customerPrivateKey = (RSAPrivateCrtKey) cardKeyPair
				.getKeys().getPrivate();
		// build the customer public key as it will be stored on the card
		// (by sending only the modulus and exponents, we save a lot of bytes,
		// so
		// we can send the pubkey in a single APDU which is more efficient)
		byte[] modulus = Formatter.getUnsignedBytes(customerPrivateKey
				.getModulus());
		byte[] pubexp = Formatter.getUnsignedBytes(customerPublicKey
				.getPublicExponent());

		// Same story for private key, only in this case we use CRT format.
		byte[] crtCoefficient = Formatter
				.getUnsignedBytes(customerPrivateKey.getCrtCoefficient());
		byte[] primeExpP = Formatter.getUnsignedBytes(customerPrivateKey
				.getPrimeExponentP());
		byte[] primeP = Formatter.getUnsignedBytes(customerPrivateKey
				.getPrimeP());
		byte[] primeExpQ = Formatter.getUnsignedBytes(customerPrivateKey
				.getPrimeExponentQ());
		byte[] primeQ = Formatter.getUnsignedBytes(customerPrivateKey
				.getPrimeQ());

		byte[] cardPublicKeyBytes = new byte[CONSTANTS.ID_LENGTH
				+ CONSTANTS.RSA_KEY_MOD_LENGTH
				+ CONSTANTS.RSA_KEY_PUBEXP_LENGTH];
		Arrays.fill(cardPublicKeyBytes, (byte) 0);
		System.arraycopy(Formatter.toByteArray(cardKeyPair.getCardID()),
				0, cardPublicKeyBytes, CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_ID,
				CONSTANTS.ID_LENGTH);
		System.arraycopy(modulus, 0, cardPublicKeyBytes,
				CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_MOD
						+ CONSTANTS.RSA_KEY_MOD_LENGTH - modulus.length,
				modulus.length);
		System.arraycopy(pubexp, 0, cardPublicKeyBytes,
				CONSTANTS.RSA_SIGNED_PUBKEY_OFFSET_EXP
						+ CONSTANTS.RSA_KEY_PUBEXP_LENGTH - pubexp.length,
				pubexp.length);

		byte[] signedCardPublicKeyBytes;
		try {
			signedCardPublicKeyBytes = office.sign(cardPublicKeyBytes);
		} catch (BackOfficeException e) {
			System.err.println("Error signing customer key: " + e.getMessage());
			office.deleteCard(cardKeyPair.getCardID());
			return;
		}

		byte[] cardPersonalizationData = new byte[CONSTANTS.PERS_MSG_LENGTH];
		Arrays.fill(cardPersonalizationData, (byte) 0);
		System.arraycopy(primeP, 0, cardPersonalizationData,
				CONSTANTS.PERS_MSG_OFFSET_PRIV_P
						+ CONSTANTS.RSA_KEY_CRT_COMP_LENGTH - primeP.length,
				primeP.length);
		System.arraycopy(primeQ, 0, cardPersonalizationData,
				CONSTANTS.PERS_MSG_OFFSET_PRIV_Q
						+ CONSTANTS.RSA_KEY_CRT_COMP_LENGTH - primeQ.length,
				primeQ.length);
		System
				.arraycopy(primeExpP, 0, cardPersonalizationData,
						CONSTANTS.PERS_MSG_OFFSET_PRIV_DP
								+ CONSTANTS.RSA_KEY_CRT_COMP_LENGTH
								- primeExpP.length, primeExpP.length);
		System
				.arraycopy(primeExpQ, 0, cardPersonalizationData,
						CONSTANTS.PERS_MSG_OFFSET_PRIV_DQ
								+ CONSTANTS.RSA_KEY_CRT_COMP_LENGTH
								- primeExpQ.length, primeExpQ.length);
		System.arraycopy(crtCoefficient, 0, cardPersonalizationData,
				CONSTANTS.PERS_MSG_OFFSET_PRIV_PQ
						+ CONSTANTS.RSA_KEY_CRT_COMP_LENGTH
						- crtCoefficient.length, crtCoefficient.length);
		System.arraycopy(signedCardPublicKeyBytes, 0, cardPersonalizationData,
				CONSTANTS.PERS_MSG_OFFSET_PUBKEY,
				CONSTANTS.RSA_SIGNED_PUBKEY_LENGTH);

		byte[] signedCardPersonalizationData;
		try {
			signedCardPersonalizationData = office
					.sign(cardPersonalizationData);
		} catch (BackOfficeException e) {
			System.err.println("Error signing card personalization data: "
					+ e.getMessage());
			office.deleteCard(cardKeyPair.getCardID());
			return;
		}

		// Send the data to the card
		if (signedCardPersonalizationData.length != (CONSTANTS.PERS_MSG_LENGTH + CONSTANTS.RSA_SIGNATURE_LENGTH)) {
			System.err
					.println("Personalization data and / or signature invalid. Aborting.");
			office.deleteCard(cardKeyPair.getCardID());
			return;
		}

		System.out.println("Sending personalization data...");

		Response response = com.sendCommand(CONSTANTS.INS_PERSONALIZE_WRITE,
				signedCardPersonalizationData);
		if (response == null) {
			System.out.println("Card already personalized.");
			office.deleteCard(cardKeyPair.getCardID());
		} else if (!response.success()) {
			System.out.println("Error: "
					+ Formatter.toHexString(response.getStatus()));
			office.deleteCard(cardKeyPair.getCardID());
		} else {
			System.out.println("Card personalized");
		}
	}

	/**
	 * Generates a new keypair for a car, and returns the new public key.
	 * 
	 * @param name
	 */
	/*
	public Car addNewCar(String name) {
		try {
			int carId = office.addNewCar(name);
			return office.getCarByID(carId);
		} catch (BackOfficeException e) {
			System.err.println("Error: Adding new car failed: "
					+ e.getMessage());
		}
		return null;
	}
	*/
	/**
	 * Register a new customer
	 * 
	 * @param cusName
	 * @param cusDateOfBirth
	 * @return
	 * @throws BackOfficeException
	 */
	public Customer registerNewCustomer(String cusName)
			throws BackOfficeException {
		return office.registerCustomer(cusName);
	}

	/**
	 * Give out a car certificate for a certain car to an inserted card.
	 * 
	 * @param carID
	 *            ID for the car that is rented
	 */
	/*
	private boolean rentCar(int carID, int numDays) {

		com.waitForCard();
		if (!session.authenticate(CONSTANTS.P1_AUTHENTICATE_OFFICE)) {
			// trying to authenticate
			System.err.println("Authentication error. Is card personalized?");
			return false;
		}
		Car theCar = null;
		try {
			theCar = office.getCarByID(carID);
		} catch (BackOfficeException e) {
			System.err.println("Error: Retrieving car keys failed: "
					+ e.getMessage());
			// e.printStackTrace();
		}
		if (theCar.getRented()) {
			String override = CLI
					.prompt("This car is already rented out. Override? (Y/N): ");

			if (!override.equals("Y")) {
				return false;
			}
		}
		// rental start and end timestamps
		int rentalStart = (int) (System.currentTimeMillis() / 1000);
		int rentalEnd = rentalStart + numDays * 68400;

		RSAPublicKey carKey;
		carKey = (RSAPublicKey) theCar.getPublicKey();

		int carSeq = theCar.getSequenceNumber() + 1;

		byte[] plaincert = new byte[CONSTANTS.CERT_PLAIN_LENGTH];
		Arrays.fill(plaincert, (byte) 0);

		int cardID = session.getCardId();

		// Add the card id
		System.arraycopy(Formatter.toByteArray(cardID), 0, plaincert,
				CONSTANTS.CERT_OFFSET_CARDID, CONSTANTS.ID_LENGTH);

		// Add the car id
		System.arraycopy(Formatter.toByteArray(carID), 0, plaincert,
				CONSTANTS.CERT_OFFSET_CARID, CONSTANTS.ID_LENGTH);

		// Add the car sequence
		System.arraycopy(Formatter.toByteArray(carSeq), 0, plaincert,
				CONSTANTS.CERT_OFFSET_SEQNUM, CONSTANTS.SEQ_LENGTH);

		// Add the startDate
		System
				.arraycopy(Formatter.toByteArray(rentalStart), 0,
						plaincert, CONSTANTS.CERT_OFFSET_START,
						CONSTANTS.DATE_LENGTH);

		// Add the endDate
		System.arraycopy(Formatter.toByteArray(rentalEnd), 0, plaincert,
				CONSTANTS.CERT_OFFSET_END, CONSTANTS.DATE_LENGTH);

		byte[] signedCert;
		try {
			signedCert = office.sign(plaincert);
		} catch (BackOfficeException e) {
			System.err.println("Error: failed to sign car certificate: "
					+ e.getMessage());
			return false;
		}

		if (signedCert.length != CONSTANTS.CERT_LENGTH) {
			System.err.println("Car certificate is of incorrect size: "
					+ signedCert.length);
			return false;
		}

		byte[] carPubkeyModulus = Formatter.getUnsignedBytes(carKey
				.getModulus());
		byte[] carPubkeyExponent = Formatter.getUnsignedBytes(carKey
				.getPublicExponent());
		byte[] rentCarMessage = new byte[CONSTANTS.RENT_MSG_LENGTH];
		System.arraycopy(carPubkeyModulus, 0, rentCarMessage,
				CONSTANTS.RENT_MSG_OFFSET_PUB_MOD
						+ CONSTANTS.RSA_KEY_MOD_LENGTH
						- carPubkeyModulus.length, carPubkeyModulus.length);
		System.arraycopy(carPubkeyExponent, 0, rentCarMessage,
				CONSTANTS.RENT_MSG_OFFSET_PUB_EXP
						+ CONSTANTS.RSA_KEY_PUBEXP_LENGTH
						- carPubkeyExponent.length, carPubkeyExponent.length);
		System.arraycopy(signedCert, 0, rentCarMessage,
				CONSTANTS.RENT_MSG_OFFSET_CERT, CONSTANTS.CERT_LENGTH);

		System.out.println("Sending certificate");
		Response response = com.sendCommand(CONSTANTS.INS_RENT_CAR,
				rentCarMessage);

		if (response == null) {
			System.err.println("Client already rented a car.");
			return false;
		}

		if (!response.success()) {
			// System.err.println("Error: " +
			// Formatter.toHexString(response.getStatus()));
			return false;
		} else {
			System.out.println("Certificate sent");
			try {
				office.incrementCarSequenceNumber(carID);
			} catch (BackOfficeException e) {
				System.err
						.println("Error: Incrementing car sequence number failed "
								+ e.getMessage() + " Revoke the certificate.");
				return false;
			}
			theCar.rentCar();
			return true;
		}
	}
	*/
	/*
	private boolean checkMileage() {
		Response mileageResponse = com.sendCommand(CONSTANTS.INS_GET_MILEAGE);

		if (mileageResponse == null) {
			System.err.println("Could not read mileage data from card.");
			return false;
		}

		byte[] mileageData = null;
		if (mileageResponse.success()) {
			mileageData = mileageResponse.getData();
		} else {
			System.err.println("Could not read mileage data from card.");
			return false;
		}

		byte[] tearFlag = new byte[mileageData.length];
		Arrays.fill(tearFlag, (byte) 0xFF);
		byte[] unused = new byte[mileageData.length];
		Arrays.fill(unused, (byte) 0x00);

		if (Arrays.equals(tearFlag, mileageData)) { // Card tear detection
			System.err
					.println("Please do not remove the card before stopping the vehicle. Please insert the card into the vehicle, and check out again.");
			return false;
		}
		if (Arrays.equals(unused, mileageData)) { // Card unused detection
			System.err
					.println("Please insert the card into the vehicle, and check out.");
			return false;
		}

		int receivedVehicleId;
		try {
			receivedVehicleId = getCarIdFromCert();
		} catch (SecurityException e) {
			CLI.showln(e.getMessage());
			return false;
		}

		KeyManager km = new KeyManager();
		RSAPublicKey carKey = null;
		try {
			carKey = (RSAPublicKey) km.loadKeys("Car_" + receivedVehicleId)
					.getPublic();
		} catch (NoSuchAlgorithmException e1) {
			System.err.println("Error while reading key of the car");
			// e1.printStackTrace();
		} catch (InvalidKeySpecException e1) {
			System.err.println("Error while reading key of the car");
			// e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			System.err.println("Error while reading key of the car");
			// e1.printStackTrace();
		} catch (IOException e1) {
			System.err.println("Error while reading key of the car");
			// e1.printStackTrace();
		}

		TerminalCrypto crypt = new TerminalCrypto();
		byte[] mileageBytes = null;
		try {
			mileageBytes = crypt.verify(mileageData, carKey);
		} catch (SignatureException e) {
			System.err.println("Error while checking the signed mileagedata");
			return false;
		}

		int mileage = Formatter.byteArrayToInt(mileageBytes);
		System.out.println("Mileage is: " + mileage);
		return true;
	}
	*/
	
	/*
	private int getCarIdFromCert() throws SecurityException {
		byte[] certificateData;
		TerminalCrypto crypto = new TerminalCrypto();
		try {
			certificateData = crypto.verify(session.getCertificate(),
					(RSAPublicKey) office.getSupermarketKeyPair().getPublic());
		} catch (SignatureException e) {
			throw new SecurityException("Invalid certificate, no car rented?");
		} catch (BackOfficeException e) {
			throw new SecurityException(e.getMessage());
		}

		byte[] vehicleIdBytes = Arrays.copyOfRange(certificateData,
				CONSTANTS.CERT_OFFSET_CARID, CONSTANTS.CERT_OFFSET_CARID
						+ CONSTANTS.ID_LENGTH);
		int receivedVehicleId = Formatter.byteArrayToInt(vehicleIdBytes);
		return receivedVehicleId;
	}
	*/

	/*
	private boolean returnCar() {
		com.waitForCard();
		if (!session.authenticate(CONSTANTS.P1_AUTHENTICATE_OFFICE)) {
			System.err.println("Authentication error.");
			return false;
		}

		System.out.println("Returning car.");
		System.out.println("Checking mileage.");

		if (checkMileage() == true) {
			// Mileage data is valid, remove the cert from the card.
			Response response = com.sendCommand(CONSTANTS.INS_RETURN_CAR);
			if (!response.success()) {
				// System.err.println("Error: " +
				// Formatter.toHexString(response.getStatus()));
				return false;
			} else {
				System.out.println("Card can no longer start the vehicle.");
				try {
					office.getCarByID(getCarIdFromCert()).returnCar();
				} catch (BackOfficeException e) {
					CLI.showln(e.getMessage());
					return false;
				}
				return true;
			}
		}
		return false;
	}
	*/

	/*
	private List<Car> getCarIds() {
		try {
			return office.getCars();
		} catch (BackOfficeException e) {
			System.err
					.println("Couldn't fetch list of cars: " + e.getMessage());
			return new ArrayList<Car>();
		}
	}
	*/

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		OfficeTerminal ot = new OfficeTerminal();

		/*
		 * Simple command line interface. We don't think that having a nice GUI
		 * is the goal here. Also, because of this, we do not "nicely" handle
		 * all invalid input.
		 */
		mainmenu: while (true) {
			String command = "0";
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("Please do not interupt me!");
			}

			command = CLI
					.prompt("\nPlease enter command.\n(1) Register new customer | (2) Personalize card | (3) View customer info | (9) Exit\n(?): ");

			if (Integer.parseInt(command) == 1) {
				String name;

				while (true) {

					String correct = "";

					name = CLI.prompt("Please enter client's name: ");

					CLI.showln("Name is " + name + ".");
					while (!correct.equals("N") && !correct.equals("Y")
							&& !correct.equals("C")) {
						correct = CLI
								.prompt("Is this correct? (Y)es/(N)o/(C)ancel: ");
					}

					if (correct.equals("C")) {
						continue mainmenu;
					} else if (correct.equals("Y")) {
						break;
					}
				}

				Customer cust;
				try {
					cust = ot.registerNewCustomer(name);
					CLI.showln("Client ID: " + cust.getID());
				} catch (BackOfficeException e) {
					System.err.println("Could not register new client.");
					continue mainmenu;
				}
				CLI.showln("Client created (Id, name): " + "(" + cust.getID()
						+ ", " + cust.getName() + ")");

			} else if (Integer.parseInt(command) == 2) {
				int client = CLI.promptInt("Please enter client's id: ");
				if (client == -1)
					continue mainmenu;

				ot.personalizeCard(client);
			} else if (Integer.parseInt(command) == 3) {
				List<Customer> customers = ot.getCustomerIds();
				for (Customer c : customers) {
					System.out.println("Customer #" + c.getID() + ": " + c.getName());
				}
				Customer chosen = null;
				while (chosen == null) {
					int cust = CLI.promptInt("Please enter customer's id: ");
					if (cust == -1)
						continue mainmenu;
					for (Customer c : customers) {
						if (c.getID() == cust) {
							chosen = c;
							break;
						}
					}
					if (chosen == null)
						System.err.println("Invalid customer");
				}
				System.out.println(chosen.getName() + ": Card ID = " + chosen.getCardID()
						+ ", balance = " + chosen.getCredits());
			}  else if (Integer.parseInt(command) == 9) {
				/* Exit program */
				ot.save();
				break;
			} else {
				System.err.println("Incorrect command entered.");
			}
		}
	}

	private List<Customer> getCustomerIds() {
		try {
			return office.getCustomers();
		} catch (BackOfficeException e) {
			System.err
					.println("Couldn't fetch list of customers: " + e.getMessage());
			return new ArrayList<Customer>();
		}
	}
}
