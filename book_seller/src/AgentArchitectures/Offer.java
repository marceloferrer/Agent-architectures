package AgentArchitectures;

//Imports for agents
import jade.core.AID;

/// Class that represent an offer
public class Offer {
	// Bidder that make the offer
	public AID Bidder;
	// Price of the offer
	public Float Price;
	
	// Create a new offer with the bidder and price information
	public Offer(AID bidder, Float price) {
		Bidder = bidder;
		Price = price;
	}
}
