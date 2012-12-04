package officeterminal;

import common.AppletSession;
import common.CLI;
import common.AppletCommunication;

import java.io.IOException;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.CardChannel;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * The office terminal is able to register new customers, delete customers and 
 * view customer info (name, card id's and balance).
 * 
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
			System.err.println("Failed to create back office simulator: " + e.getMessage());
			return;
		}

		try {
			RSAPrivateKey supermarketPrivateKey = (RSAPrivateKey) office.getSupermarketKeyPair().getPrivate();
			RSAPublicKey supermarketPublicKey = (RSAPublicKey) office.getSupermarketKeyPair().getPublic();
			session = new AppletSession(supermarketPublicKey, supermarketPrivateKey, 0);
		} catch (BackOfficeException e) {
			System.err.println("Failed to fetch supermarket private key: " + e.getMessage());
		}
		com = new AppletCommunication(session);
	}

	/**
	 * Get the customer from the database with the corresponding id from this session
	 * 
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
			System.err.println("Failed to save BackOfficeSimulator state: " + e.getMessage());
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
	public Customer registerNewCustomer(String cusName) throws BackOfficeException {
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

			command = CLI.prompt("\nPlease enter command.\n(1) Register new customer |"
					+ " (2) View customer info | (3) Delete customer | (9) Exit\n(?): ");

			/* Register new customer */
			if (Integer.parseInt(command) == 1) {
				String name;

				while (true) {

					String correct = "";

					name = CLI.prompt("Please enter client's name: ");

					CLI.showln("Name is " + name + ".");
					while (!correct.equals("N") && !correct.equals("Y") && !correct.equals("C")) {
						correct = CLI.prompt("Is this correct? (Y)es/(N)o/(C)ancel: ");
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
				CLI.showln("Client created (Id, name): " + "(" + cust.getID() + ", " + cust.getName() + ")");

			}

			/* View more info about specific customer */
			else if (Integer.parseInt(command) == 2) {
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
				System.out.println(chosen.getName() + ": Card ID = " + chosen.getCardID() + ", balance = " + chosen.getCredits());

			}

			/* Delete a specific customer */
			else if (Integer.parseInt(command) == 3) {
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
				} catch (BackOfficeException e) {
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
	 * 
	 * @param cust
	 *            idof customer to be fetched
	 * @return customer of type Customer
	 * @throws BackOfficeException
	 */
	private Customer getCustomerByID(int cust) throws BackOfficeException {
		return office.getCustomerByID(cust);
	}

	/**
	 * Delete a specified customer from the database
	 * 
	 * @param customerID
	 *            id of the customer to be removed from the database
	 * @throws BackOfficeException
	 */
	private void deleteCustomer(int customerID) throws BackOfficeException {
		office.deleteCustomer(customerID);
	}

	/**
	 * Fetch a list of all customers in the database
	 * 
	 * @return an ArrayList with customers of type Customer
	 */
	private List<Customer> getCustomerIds() {
		try {
			return office.getCustomers();
		} catch (BackOfficeException e) {
			System.err.println("Couldn't fetch list of customers: " + e.getMessage());
			return new ArrayList<Customer>();
		}
	}
}
