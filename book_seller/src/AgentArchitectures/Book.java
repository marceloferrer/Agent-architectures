package AgentArchitectures;

import java.io.Serializable;

// Class that represent a book. Its serializable so it can be used inside the messages
public class Book implements Serializable{
	public String Name;
	public Float Initial_Price;
	public Float Current_Price;
	public String Buyer_Name;
	
	public Book(String name, Float price) {
		Name = name;
		Initial_Price = price;
	}
	
	// Set the buyer of the book
	public void Set_Buyer(String buyer, Float price) {
		Buyer_Name = buyer;
		Current_Price = price;
	}
}
