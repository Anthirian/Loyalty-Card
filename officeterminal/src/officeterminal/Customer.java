package officeterminal;

import java.io.Serializable;

/**
 * Internal representation of a customer.
 * 
 * @author Geert Smelt
 * @author Robin Oostrum
 * 
 */
public class Customer implements Serializable {

	private static final long serialVersionUID = 7401122668539562560L;
	private String name;
	private int id;
	private Card currentCard = null;

	public Customer clone() {
		Customer c = new Customer(name, id);
		c.setCard(currentCard);
		return c;
	}

	String getName() {
		return name;
	}

	int getID() {
		return id;
	}

	public void setCard(Card card) {
		this.currentCard = card;
	}

	int getCardID() {
		return currentCard.getID();
	}

	short getCredits () {
		return currentCard.getCredits();
	}
	
	Customer(String name, int id) {
		this.name = name;
		this.id = id;
	}
}
