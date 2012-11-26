package officeterminal;

import java.util.List;
import java.security.KeyPair;

/**
 * Interface for communicating with BackOffice.
 * 
 * @author Pol Van Aubel (paubel@science.ru.nl)
 * @author Marlon Baeten (mbaeten@science.ru.nl)
 * @author Sjors Gielen (sgielen@science.ru.nl)
 * @author Robert Kleinpenning (rkleinpe@science.ru.nl)
 * @author Jille Timmermans (jilletim@science.ru.nl)
 */
interface BackOffice {
	Customer registerCustomer(String cusName) throws BackOfficeException;

	KeyPair getSupermarketKeyPair() throws BackOfficeException;

	IDKeyPair issueNewCard(Customer client) throws BackOfficeException;

	void deleteCard(int cardID);
	
	void deleteCustomer(int customerID);

	//int addNewCar(String name) throws BackOfficeException;

	//Car getCarByID(int carId) throws BackOfficeException;

	// void incrementCarSequenceNumber(int carId) throws BackOfficeException;

	//List<Car> getCars() throws BackOfficeException;

	int getCardID(Customer client) throws BackOfficeException;
	
	short getCredits(Customer client) throws BackOfficeException;
	
	Customer getCustomerByID(int custID) throws BackOfficeException;

	Customer getCustomerByName(String name) throws BackOfficeException;

	Customer getCustomerByCard(int cardID) throws BackOfficeException;

	byte[] sign(byte[] obj) throws BackOfficeException;

	List<Customer> getCustomers() throws BackOfficeException;
}
