package officeterminal;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import common.TerminalCrypto;
import common.KeyManager;
import common.Formatter;

/**
 * Simulates the BackOffice.
 * 
 * @author Pol Van Aubel (paubel@science.ru.nl)
 * @author Marlon Baeten (mbaeten@science.ru.nl)
 * @author Sjors Gielen (sgielen@science.ru.nl)
 * @author Robert Kleinpenning (rkleinpe@science.ru.nl)
 * @author Jille Timmermans (jilletim@science.ru.nl)
 */
class BackOfficeSimulator implements BackOffice {
	private Hashtable<Integer, Customer> customers;
	private Hashtable<Integer, Card> cards;
	//private Hashtable<Integer, Car> cars;
	private KeyManager keymanager;
	private TerminalCrypto crypto;
	private KeyPair supermarketKeyPair;
	private int lastCustomerId = 0;
	private int lastCardId = 0;
	//private int lastCarId = 0;
	private static final String keyExtension = "supermarket";

	public BackOfficeSimulator(String path) throws BackOfficeException {
		keymanager = new KeyManager(path);
		crypto = new TerminalCrypto();
		boolean backofficeLoaded = false;

		if (new File("backoffice.db").exists()) {
			try {
				loadBackOffice();
				backofficeLoaded = true;
			} catch (IOException e) {
				System.err.println("Failed to load backoffice simulator data: "
						+ e.getMessage());
			} catch (ClassNotFoundException e) {
				System.err.println("Failed to load backoffice simulator data: "
						+ e.getMessage());
			} catch (BackOfficeException e) {
				throw e;
			}
		}

		if (!backofficeLoaded) {
			System.out
					.println("Loading information failed. Generating new information...");
			try {
				if (new File(path + "privkey_rsa.supermarket").exists()) {
					System.out
							.println("Trying to load existing supermarket key...");
					supermarketKeyPair = keymanager.loadKeys(keyExtension);
				} else {
					System.out.println("Trying to generate new supermarket key...");
					supermarketKeyPair = keymanager.generateAndSave(keyExtension);
				}
			} catch (NoSuchAlgorithmException e) {
				throw new BackOfficeException(
						"Failed to generate or load supermarket key: "
								+ e.getMessage());
			} catch (InvalidKeySpecException e) {
				throw new BackOfficeException("Failed to load supermarket key: "
						+ e.getMessage());
			} catch (IOException e) {
				throw new BackOfficeException(
						"Failed to generate or load supermarket key: "
								+ e.getMessage());
			}
			customers = new Hashtable<Integer, Customer>();
			cards = new Hashtable<Integer, Card>();
			//cars = new Hashtable<Integer, Car>();
			customers.put(0, new Customer("Robin Oostrum", 0));
			lastCustomerId = 0;
			lastCardId = 0;
			//lastCarId = 0;
			remoteSave();
		}

		System.out.println("Loaded information on " + lastCustomerId
				+ " customers, and " + lastCardId + " cards.");
	}

	private void remoteSave() throws BackOfficeException {
		try {
			save();
		} catch (IOException e) {
			throw new BackOfficeException("Saving backoffice state failed", e);
		}
	}

	public void save() throws IOException {
		FileOutputStream fos = new FileOutputStream("backoffice.db");
		ObjectOutputStream out = new ObjectOutputStream(fos);
		System.out.println("Saving information on " + lastCustomerId
				+ " customers, and " + lastCardId + " cards.");
		out.writeInt(lastCustomerId);
		out.writeInt(lastCardId);
		//out.writeInt(lastCarId);
		out.writeObject(customers);
		out.writeObject(cards);
		//out.writeObject(cars);
	}

	// If this fails, we need to exit anyway, so we suppress these warnings.
	@SuppressWarnings("unchecked")
	private void loadBackOffice() throws IOException, ClassNotFoundException,
			BackOfficeException {
		try {
			supermarketKeyPair = keymanager.loadKeys(keyExtension);
		} catch (NoSuchAlgorithmException e) {
			throw new BackOfficeException("Failed to load supermarket key", e);
		} catch (InvalidKeySpecException e) {
			throw new BackOfficeException("Failed to load supermarket key", e);
		} catch (IOException e) {
			throw new BackOfficeException("Failed to load supermarket key", e);
		}

		FileInputStream fis = new FileInputStream("backoffice.db");
		ObjectInputStream in = new ObjectInputStream(fis);
		lastCustomerId = in.readInt();
		lastCardId = in.readInt();
		//lastCarId = in.readInt();
		customers = (Hashtable<Integer, Customer>) in.readObject();
		cards = (Hashtable<Integer, Card>) in.readObject();
		//cars = (Hashtable<Integer, Car>) in.readObject();
	}

	@Override
	public KeyPair getSupermarketKeyPair() throws BackOfficeException {
		return supermarketKeyPair;
	}

	/*
	@Override
	public int addNewCar(String name) throws BackOfficeException {
		int carSeqId = ++lastCarId;

		KeyPair keypair;
		try {
			keypair = keymanager.generateAndSave("Car_"
					+ Integer.toString(carSeqId));
		} catch (NoSuchAlgorithmException e) {
			throw new BackOfficeException("Failed to generate a new car key", e);
		} catch (IOException e) {
			throw new BackOfficeException("Failed to generate a new car key", e);
		}

		try {
			writeCarSequence(carSeqId, 0);
		} catch (IOException e) {
			throw new BackOfficeException("Failed to update car state", e);
		}

		Car car = new Car(name, carSeqId, (RSAPublicKey) keypair.getPublic());
		cars.put(carSeqId, car);
		remoteSave();

		return carSeqId;
	}

	@Override
	public Car getCarByID(int carId) throws BackOfficeException {
		Car c = cars.get(carId);
		if (c == null) {
			return c;
		}
		return c.clone();
	}

	@Override
	public void incrementCarSequenceNumber(int carId)
			throws BackOfficeException {
		Car c = cars.get(carId);
		if (c == null) {
			throw new BackOfficeException("No car with that ID");
		}
		c.incrementSequenceNumber();
		remoteSave();
	}
	*/

	@Override
	public int getCardID(Customer client) throws BackOfficeException {
		return client.getCardID();
	}

	@Override
	public Customer getCustomerByCard(int cardID) throws BackOfficeException {
		return cards.get(cardID).getCustomer();
	}

	@Override
	public Customer getCustomerByID(int custID) throws BackOfficeException {
		Customer c = customers.get(custID);
		if (c == null) {
			throw new BackOfficeException("Unkown Customer.");
		}
		return c.clone();
	}

	@Override
	public Customer getCustomerByName(String name) throws BackOfficeException {
		Enumeration<Integer> keys = customers.keys();
		while (keys.hasMoreElements()) {
			int key = keys.nextElement();
			Customer cust = customers.get(key);
			if (cust.getName().equals(name)) {
				return cust;
			}
		}
		return null;
	}

	@Override
	public IDKeyPair issueNewCard(Customer client) throws BackOfficeException {
		int cardId = ++lastCardId;

		String ident = "Card_" + cardId;
		KeyPair cardKeyPair;
		try {
			cardKeyPair = keymanager.generateAndSave(ident);
		} catch (IOException e) {
			throw new BackOfficeException("Could not save new card key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new BackOfficeException("Could not save new card key", e);
		}

		RSAPublicKey cardPublicKey = (RSAPublicKey) cardKeyPair.getPublic();
		Card newCard = new Card(cardPublicKey, cardId, (short) 0, client);
		client.setCard(newCard);
		cards.put(cardId, newCard);
		remoteSave();

		IDKeyPair idKeys = new IDKeyPair(cardId, cardKeyPair);

		return idKeys;
	}

	/**
	 * Remove a certain card from the database
	 * 
	 * @param cardId
	 */
	public void deleteCard(int cardId) {
		Card card = cards.get(cardId);
		if (card == null) {
			System.err.println("Cannot remove non-existing card with id: "
					+ cardId);
			return;
		}
		Customer cust = card.getCustomer();
		cust.setCard(null);
		cards.remove(cardId);
		--lastCardId;

		try {
			remoteSave();
		} catch (BackOfficeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Register a new customer, based on name and date of birth
	 */
	public Customer registerCustomer(String cusName) throws BackOfficeException {
		int customerId = ++lastCustomerId;
		Customer newCustomer = new Customer(cusName, customerId);
		customers.put(customerId, newCustomer);
		remoteSave();

		return newCustomer;
	}

	@Override
	public byte[] sign(byte[] obj) throws BackOfficeException {
		byte[] signedCert = crypto.sign(obj, (RSAPrivateKey) supermarketKeyPair
				.getPrivate());
		if (signedCert == null) {
			throw new BackOfficeException(
					"Failed to sign incoming data from front-office terminal");
		}
		return signedCert;
	}

	@Override
	public short getCredits(Customer client) throws BackOfficeException {
		return client.getCredits();
	}

	@Override
	public List<Customer> getCustomers() throws BackOfficeException {
		ArrayList<Customer> list = new ArrayList<Customer>();
		for (Enumeration<Customer> e = customers.elements(); e.hasMoreElements();) {
			Customer c = e.nextElement();
			list.add(c.clone());
		}
		return list;
	}

	@Override
	public void deleteCustomer(int customerID) {
		Customer customer = customers.get(customerID);
		if (customer == null) {
			System.err.println("Cannot remove non-existing customer with id: "
					+ customerID);
			return;
		}
		
		//deleteCard(customer.getCardID());
		customers.remove(customerID);
		--lastCustomerId;

		try {
			remoteSave();
		} catch (BackOfficeException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Write a new sequence number for given car ID.
	 */
	/*
	private void writeCarSequence(int carID, int seq) throws IOException {
		FileOutputStream csfh = new FileOutputStream("./keys/carseq_" + carID);
		csfh.write(RentalFormatter.toByteArray(seq));
		csfh.close();
	}

	@Override
	public List<Car> getCars() {
		ArrayList<Car> list = new ArrayList<Car>();
		for (Enumeration<Car> e = cars.elements(); e.hasMoreElements();) {
			Car c = e.nextElement();
			list.add(c.clone());
		}
		return list;
	}
	*/
}
