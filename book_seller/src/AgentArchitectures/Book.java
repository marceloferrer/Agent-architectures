package AgentArchitectures;

// Imports to make the class serializable
import java.io.Serializable;

// Class that represent a book. Its serializable so it can be passed inside the messages.
@SuppressWarnings("serial")
public class Book implements Serializable{
	// Name of the book
	public String Name;
	// Initial price of the book
	public Float Initial_Price;
	// Current price of the book. When the auction is finished this will be the final price.
	public Float Current_Price;
	// Name of the bidder agent that
	public String Buyer_Name;
	
	// Create a new book
	public Book(String name, Float price) {
		Name = name;
		Initial_Price = price;
	}
	
	// Set the buyer of the book and the final price
	public void Set_Buyer(String buyer, Float price) {
		Buyer_Name = buyer;
		Current_Price = price;
	}
}
