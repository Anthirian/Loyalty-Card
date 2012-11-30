package officeterminal;

import java.util.List;
import java.security.KeyPair;

/**
 * Interface for communicating with BackOffice.
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 */
interface BackOffice {
	Customer registerCustomer(String cusName) throws BackOfficeException;

	KeyPair getSupermarketKeyPair() throws BackOfficeException;

	IDKeyPair issueNewCard(Customer client) throws BackOfficeException;

	void deleteCard(int cardID);
	
	void deleteCustomer(int customerID);

	int getCardID(Customer client) throws BackOfficeException;
	
	short getCredits(Customer client) throws BackOfficeException;
	
	Customer getCustomerByID(int custID) throws BackOfficeException;

	Customer getCustomerByName(String name) throws BackOfficeException;

	Customer getCustomerByCard(int cardID) throws BackOfficeException;

	byte[] sign(byte[] obj) throws BackOfficeException;

	List<Customer> getCustomers() throws BackOfficeException;
}
