package officeterminal;

import common.AppletSession;
import common.CLI;
import common.CONSTANTS;
import common.Formatter;
import common.Response;
import common.AppletCommunication;

import java.io.IOException;
import java.security.Security;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.CardChannel;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * The office terminal is able to register new customers, personalize cards, delete
 * customers and view customer info (name, card id's and balance).
 * @author Robin Oostrum
 * @author Geert Smelt
 *
 */
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

	/**
	 * Get the customer from the database with the corresponding id from this session
	 * @return customer with id from this session
	 */
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

	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());
		OfficeTerminal ot = new OfficeTerminal();

		/* Simple command line interface */
		mainmenu: while (true) {
			String command = "0";
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("Please do not interupt me!");
			}

			command = CLI
					.prompt("\nPlease enter command.\n(1) Register new customer |" +
							" (2) Personalize card | (3) View customer info |" +
							" (4) Delete customer | (9) Exit\n(?): ");

			/* Register new customer */
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

			}
			
			/* Personalize card: create and store keys on the card */
			else if (Integer.parseInt(command) == 2) {
				int client = Integer.parseInt(CLI.prompt("Please enter client's id: "));
				if (client == -1)
					continue mainmenu;
				ot.personalizeCard(client);
			}
			
			/* View more info about specific customer */
			else if (Integer.parseInt(command) == 3) {
				List<Customer> customers = ot.getCustomerIds();
				for (Customer c : customers) {
					System.out.println("Customer #" + c.getID() + ": " + c.getName());
				}
				Customer chosen = null;
				while (chosen == null) {
					int cust = Integer.parseInt(CLI.prompt("Please enter customer's id: "));
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
				
			}
			
			/* Delete a specific customer */
			else if (Integer.parseInt(command) == 4) {
				int cust = Integer.parseInt(CLI.prompt("Please enter customer's id: "));
				if (cust == -1)
					continue mainmenu;
				try {
					Customer c = ot.getCustomerByID(cust);
					CLI.showln("Selected customer: " + c.getName());
				} catch (BackOfficeException e) {
					System.err.print("Invalid customer");
				}
				CLI.checkInt(cust);
				try {
					ot.deleteCustomer(cust);
					CLI.showln("Removed Customer with ID = " + cust + " from database.");
				}
				catch (BackOfficeException e) {
					System.err.println("Could not delete client.");
					continue mainmenu;
				}
			}
			
			/* Exit */
			else if (Integer.parseInt(command) == 9) {
				ot.save();
				break;
			} else {
				System.err.println("Incorrect command entered.");
			}
		}
	}

	/**
	 * Returns a customer with specified customer id
	 * @param cust idof customer to be fetched
	 * @return customer of type Customer
	 * @throws BackOfficeException
	 */
	private Customer getCustomerByID(int cust) throws BackOfficeException {
		return office.getCustomerByID(cust);
	}

	/**
	 * Delete a specified customer from the database
	 * @param customerID id of the customer to be removed from the database
	 * @throws BackOfficeException
	 */
	private void deleteCustomer(int customerID) throws BackOfficeException {
		office.deleteCustomer(customerID);
	}

	/**
	 * Fetch a list of all customers in the database
	 * @return an ArrayList with customers of type Customer
	 */
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
