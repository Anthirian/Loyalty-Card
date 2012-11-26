package officeterminal;

import java.io.Serializable;

/**
 * Internal representation of a customer.
 * 
 * @author Pol Van Aubel (paubel@science.ru.nl)
 * @author Marlon Baeten (mbaeten@science.ru.nl)
 * @author Sjors Gielen (sgielen@science.ru.nl)
 * @author Robert Kleinpenning (rkleinpe@science.ru.nl)
 * @author Jille Timmermans (jilletim@science.ru.nl)
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
