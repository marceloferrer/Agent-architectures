package AgentArchitectures;

//Imports for file manipulation
import java.io.FileWriter;

//Imports for generic behaviour
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

//Imports for agents
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

//Class in charge of bidder behavior
@SuppressWarnings("serial")
public class BidderAgent extends Agent{
	// Instance of random class
	Random rand = new Random(); 

	// Hash table with the interest in each book
	private Hashtable<String, Integer> interests;
	// Level of aggressiveness of the bidder
	private int aggressiveness;
	// Budget of the bidder
	private float budget;
	// Type of the Agent
	private int agentType;
	
	// Number of offer for the current auction
	private ArrayList<Float> previous_offers;

	// List of purchased books
	private ArrayList<Book> purchased_books; 
	
	// Copy of Budget of the bidder to use in print of file
	private float initial_budget;	
	// Copy of Hash table with the interest in each book to use in print of file
	private ArrayList<Integer> interests_copy;
		
	// Initialization execution of the bidder agent
	protected void setup() {
		Utils.print_msg(getAID().getName(), "Starting Bidding agent.");

		// Configure the initial values of the agent
		interests = new Hashtable<String, Integer>();	
		interests_copy = new ArrayList<Integer>();
		purchased_books = new ArrayList<Book>();
		previous_offers = new ArrayList<Float>();
		// Agent type reactive by default
		agentType = 0;
		// Random value between 0 and 10 (aggressiveness only used in reactive agent). If its provided as an argument, it will be replaced.
		aggressiveness = rand.nextInt(11);
		// Random budget. If its provided as an argument, it will be replaced.
		budget = rand.nextInt(1000, 100000);
		
		// Get the configuration passed as arguments
		try {
			Object[] args = getArguments();
			
			if (args != null && args.length > 0) {
				String type = (String)args[0];
								
				agentType = Integer.parseInt(type);
				
				if (args.length > 1) {
					String bdg = (String)args[1];
					budget = Float.parseFloat(bdg);
					
					if (args.length > 2) {
						String agr = (String)args[2];
						aggressiveness = Integer.parseInt(agr);
					}
				}
			}
		}
		catch (Exception ex) {
			Utils.print_msg(getAID().getName(), "Error in getting agent type from arguments");
			ex.printStackTrace();
		}
		initial_budget = budget;
		Utils.print_msg(getAID().getName(), "Type: " + agentType + ". Budget: " + budget + ". Aggressiveness: " + aggressiveness);
		
		// Check if budget inside the configuration
		if (budget >= 1000 && budget <= 100000) {
			// Get the auctioneer
			try {
				// Get the auctioneer agent from the directory
				AID auctioneer = getAuctioneer();
				
				// If the auctioneer have not started yet, wait a second and a half
				while (auctioneer == null) {
					Thread.sleep(1500);
					auctioneer = getAuctioneer();
				}
								
				// Send message to the auctioneer requesting the catalog with a Call For Proposal message
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				cfp.addReceiver(auctioneer);
				cfp.setConversationId("Catalogue-request-" + getAID().getName());
				this.send(cfp);
			}
			catch (Exception ex) {
				Utils.print_msg(getAID().getName(), Utils.RED + "Error in starting behavior of Bidder" + Utils.RESET);
				ex.printStackTrace();
			}
			
			// Add the behaviour for the expected messages. 
			// Done this way to simplify the code but less behaviours with more complex functionality could been implemented.
			addBehaviour(new CatalogueResponseServer());
			addBehaviour(new AuctionResponseServer());
			addBehaviour(new PurchaseResponseServer());
			addBehaviour(new AuctionCancelResponseServer());
		}
		else {
			Utils.print_msg(getAID().getName(), Utils.RED + "Budget of " + budget + " is outside the budget configuration." + Utils.RESET);
		}
	}
	
	/// Get the actioneer using the DF (Directory Facilitator)
	private AID getAuctioneer() {
		AID res = null;
		// Get the agent of type auctioneer
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("auctioneer");
		template.addServices(sd);
		
		try {
			// Search in the yellow pages for the auctioneer
			DFAgentDescription[] result = DFService.search(this, template); 
			
			// Check if the auctioneer could been found
			if (result.length > 0)
			{
				res = result[0].getName();
				Utils.print_msg(getAID().getName(), "Auctioneer found");
			}
			else {
				Utils.print_msg(getAID().getName(), "Auctioneer not registered!!!");
			}
		}
		catch (FIPAException fe) {
			Utils.print_msg(getAID().getName(), "Error in starting behavior of Bidder");
			fe.printStackTrace();
		}
		
		return res;
	}
	
	// Agent is been close. Inform in the console
	protected void takeDown() {		
		Utils.print_msg(getAID().getName(), "Closing Bidder agent " + getAID().getName() + ".");
		Utils.print_msg(getAID().getName(), "Remaining budget: " + budget);
		
		// Bidder is not registered, so no need to Unregister
	}
	
	/// Behaviour for the inform message. Receives the catalog and adds an interest to each item.
	private class CatalogueResponseServer extends CyclicBehaviour {
		
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			
			// If the recieved message is a inform message
			if (msg != null) {
				System.out.println("Catalogue response recieved from:" + msg.getSender().getName());
				
				try {
					// Get the content of the message (arraylist of book, disable warning because we know the content)
					@SuppressWarnings("unchecked")
					ArrayList<Book> catalog = (ArrayList<Book>) msg.getContentObject();
					
					// For each book in the catalog
					for (int i = 0; i < catalog.size(); ++i) {
						// Give a random interest to each book in the catalog. 
						int in = rand.nextInt(11);
						
						// Note that this interest will be "hidden" to the reactive agent. 
						// It will only take into account the current book been auctioned
						interests.put(catalog.get(i).Name, in);
						interests_copy.add(in);
						
						Utils.print_msg(getAID().getName(), "Interest " + interests.get(catalog.get(i).Name) + " in " + catalog.get(i).Name);
					}
				} catch (Exception e) {
					Utils.print_msg(getAID().getName(), "Error at processing catalogue response");
					e.printStackTrace();
				}
			}
			else {
				// Blocks this behaviour, is not needed for now
				block();
			}
		}
	}
	
	///Behaviour for the Call For Proposal message. Receives the auction announcement and make offers if it can.
	private class AuctionResponseServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Auction response recieved from:" + msg.getSender().getName());
				
				try {
					Book book = (Book) msg.getContentObject();
					//Utils.print_msg(getAID().getName(), "Book auction:" + book.Name);
					previous_offers.add(book.Current_Price);
						
					// Ig agent type is reactive
					if (agentType == 0) {
						// Formulas of the reactive agent:
						// (max_number_offfers = I).
						// offer = current_price+(current_price*((A+I)/20))
						
						Utils.print_msg(getAID().getName(), "Book price: " + book.Current_Price + 
								". Aggressiveness:" + aggressiveness +
								". Interest:" + interests.get(book.Name) + 
								". Agent Type:" + agentType +
								". Budget:" + budget +
								". Round:" + previous_offers.size());
						
						// Calculate new offers. Split in different lines to simplify code
						float tot_IA = aggressiveness + interests.get(book.Name);
						float new_offer = book.Current_Price + book.Current_Price * (tot_IA/20);
						// Round to two decimals
						new_offer = (float) (Math.round(new_offer * 100.0) / 100.0);
						
						// Reactive does not know its budget till here to check if it has exceeded
						if (new_offer > budget) {
							Utils.print_msg(getAID().getName(), "Budget of " + budget + " exceeded, rejecting proposal!!!");
							previous_offers = new ArrayList<Float>();
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							interests.remove(book.Name);
							
							myAgent.send(reply);
						}
						// Reactive only offers the total rounds it has interest in
						else if (previous_offers.size() > interests.get(book.Name)) {
							Utils.print_msg(getAID().getName(), "Reached limit of " + interests.get(book.Name) + " interest, rejecting proposal!!!");
							previous_offers = new ArrayList<Float>();
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							interests.remove(book.Name);
							
							myAgent.send(reply);
						}
						// If no validation break, propose the offer
						else
						{
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.PROPOSE);						
							reply.setContentObject(new_offer);
							
							myAgent.send(reply);
						}
					}
					// If agent is deliberative
					else {
						// Formulas for the deliberative agent:
						// max_value = B*(I/Sum(remain_items))
						// offer = last_offer + (Sum(previous_differences)*(I/10))
						
						// Calculate the new offer based in the previous different offers and the interest
						// Bidding is not influenced by its aggressiveness parameter
						// Split in different lines to simplify code
						float previous_differences = getPreviousDifference(book);
						float in = (float)interests.get(book.Name)/10;
						float new_offer = book.Current_Price + (previous_differences * in);
						
						// Round to two decimals
						new_offer = (float) (Math.round(new_offer * 100.0) / 100.0);
						
						float max_value = 0;
						if (interests.get(book.Name) > 0) {
							// Calculate the max value to spend based on the interest and the remaining budget
							// Split in different lines to simplify code
							int remaining_interest = getRemainingInterest();
							float tot_RA = (float)interests.get(book.Name)/remaining_interest;
							max_value = budget*(tot_RA);
						}
						
						Utils.print_msg(getAID().getName(), "Book price: " + book.Current_Price + 
								". Interest:" + interests.get(book.Name) + 
								". Agent Type:" + agentType +
								". Budget:" + budget +
								". Previous differences:" + previous_differences +
								". Max Value:" + max_value);
						
						// Check if the offer does not exceed the total budget of the agent
						if (new_offer > budget) {
							Utils.print_msg(getAID().getName(), "Budget of " + budget + " exceeded, rejecting proposal!!!");
							
							previous_offers = new ArrayList<Float>();
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							interests.remove(book.Name);
							
							myAgent.send(reply);
						}
						// Check if the offer does not exceed the max budget calculated for this auction
						else if (new_offer > max_value) {
							Utils.print_msg(getAID().getName(), "Reached limit of maximum value " + max_value + " for this book, rejecting proposal!!!");
							
							previous_offers = new ArrayList<Float>();
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							interests.remove(book.Name);
							
							myAgent.send(reply);
						}
						// If no validation break, propose the offer
						else
						{
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.PROPOSE);						
							reply.setContentObject(new_offer);
							
							myAgent.send(reply);
						}
					}
				} catch (Exception e) {
					Utils.print_msg(getAID().getName(), "Error at processing catalogue response");
					e.printStackTrace();
				}
			}
			else {
				// Blocks this behaviour, is not needed for now
				block();
			}
		}
		
		/// Get the previous differences
		private float getPreviousDifference(Book book) {
			// If there are previous offers
			if (previous_offers.size() > 1) {
				float tot = 0;
				for (int i = 0; i < previous_offers.size(); ++i) {
					// If there is a next price
					if (previous_offers.size() > i + 1) {
						tot = tot + (previous_offers.get(i+1) - previous_offers.get(i));
					}
				}
				// In case that the total could not been calculated for some error (should not happend), return 1
				if (tot > 0) {
					return tot;
				}
				else {
					return 1;
				}
			}
			// If is the initial offer, just use that initial value
			else {
				return previous_offers.get(0);
			}
		}
		
		/// Get the interest of the remaining items
		private int getRemainingInterest() {
			int total_interest = 0;

			// For each pending auction, sum the interest
			for (int i = 0; i < interests.size(); ++i) {
				Object key = interests.keySet().toArray()[i];
				total_interest = total_interest + interests.get(key);			
			}	
			
			return total_interest;
		}
	}
	
	///Behaviour for the Call For Accept proposal message. Receives the auction purchase message and store the purchase.
	private class PurchaseResponseServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				Utils.print_msg(getAID().getName(), "Auction response recieved from:" + msg.getSender().getName());
				
				try {
					// Get the book and store in local memory
					Book book = (Book) msg.getContentObject();
					Utils.print_msg(getAID().getName(), "Book purchase:" + book.Name + " $" + book.Current_Price);
					
					// Change the states of the variables
					previous_offers = new ArrayList<Float>();
					purchased_books.add(book);
					interests.remove(book.Name);
					
					// Remove the used money from the budget
					budget = budget - book.Current_Price;
					
				} catch (Exception e) {
					Utils.print_msg(getAID().getName(), "Error at processing catalogue response");
					e.printStackTrace();
				}
			}
			else {
				// Blocks this behaviour, is not needed for now
				block();
			}
		}
	}
	
	///Behaviour for the Cancel message. Receives the auction cancel message (auction finished) and save the purchase file.
	private class AuctionCancelResponseServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				previous_offers = new ArrayList<Float>();
				Utils.print_msg(getAID().getName(), "Auction finished!!!");
				
				// Show in console all the purchases made by the agent and store them in a file.
				try {
					Utils.print_msg(getAID().getName(), "BOOK NAME - PURCHASE PRICE");
					String file_name = getAID().getName().split("@")[0];
					
					FileWriter myWriter = new FileWriter(Utils.Path + file_name + ".txt");
					myWriter.write("PURCHASE");
					myWriter.write(System.lineSeparator());
					myWriter.write("BOOK NAME - PRICE");
					
					for (int i = 0; i < purchased_books.size(); ++i) {
						myWriter.write(System.lineSeparator());
						String log = purchased_books.get(i).Name + " - " + purchased_books.get(i).Current_Price;
						myWriter.write(log);
						Utils.print_msg(getAID().getName(), log);
					}  
					
					myWriter.write(System.lineSeparator());
					myWriter.write("Remaining budget: " + budget);
					myWriter.write(System.lineSeparator() + System.lineSeparator());
					myWriter.write("INITIAL CONFIGURATION");
					myWriter.write(System.lineSeparator());
					myWriter.write("Interest (TO UNIFY IN EXCEL FOR REPORT)");
					
					for (int i = 0; i < interests_copy.size(); ++i) {
						myWriter.write(System.lineSeparator());
						myWriter.write(String.valueOf(interests_copy.get(i)));
					}
					
					myWriter.write(System.lineSeparator());
					myWriter.write("Initial Budget: " + initial_budget);
					myWriter.write(System.lineSeparator());
					myWriter.write("Aggressiveness: " + aggressiveness);
					myWriter.close();
			    } 
				catch (Exception e) {
					Utils.print_msg(getAID().getName(), "Error finishing bidder");
					e.printStackTrace();
			    }
			}
			else {
				// Blocks this behaviour, is not needed for now
				block();
			}
		}
	}
}