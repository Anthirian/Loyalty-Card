package officeterminal;

import java.io.Serializable;
import java.security.interfaces.RSAPublicKey;

/**
 * Internal representation of a card.
 * 
 * @author Pol Van Aubel (paubel@science.ru.nl)
 * @author Marlon Baeten (mbaeten@science.ru.nl)
 * @author Sjors Gielen (sgielen@science.ru.nl)
 * @author Robert Kleinpenning (rkleinpe@science.ru.nl)
 * @author Jille Timmermans (jilletim@science.ru.nl)
 * 
 */
public class Card implements Serializable {

	private static final long serialVersionUID = -7112167796394873889L;
	private int id;
	private short credits;
	private Customer customer;
	private RSAPublicKey publicKey;

	Card(RSAPublicKey publicKey, int cardId, short credits, Customer customer) {
		this.publicKey = publicKey;
		this.id = cardId;
		this.credits = credits;
		this.customer = customer;
	}

	public Card clone() {
		return new Card(publicKey, id, credits, customer);
	}

	Customer getCustomer() {
		return customer;
	}

	int getCustomerID() {
		return customer.getID();
	}

	int getID() {
		return id;
	}
	
	short getCredits () {
		return credits;
	}
}