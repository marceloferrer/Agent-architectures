package AgentArchitectures;

// Imports for agents
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

// Imports for file manipulation
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

// Imports for generic behaviour
import java.util.*;

// Class in charge of the auction. Supress warning.
@SuppressWarnings("serial")
public class AuctionAgent extends Agent {
	// Constant to define how much time sould pass between each auction step.
	Integer timer = 3000;
	
	// The catalog of books for sale (maps the title of a book to its price)
	private ArrayList<Book> catalogue;
	// The list of possible bidders
	private ArrayList<AID> bidders;
	// The current book in auction
	private Book current_book;
	// The current best offer in auction
	private Offer current_best_offer;
	// The current offers in auction
	private ArrayList<Offer> current_offers;
	// The current bidders in auction
	private ArrayList<AID> current_bidders;
	// The catalog of books which auction finish
	private ArrayList<Book> finished_catalogue;
	// Mark that the auction has finished
	boolean finished;
	
	// Initialization execution of the auctioneer agent
	protected void setup() {
		Utils.print_msg(getAID().getName(), "Starting Auction agent");
		
		// Configure the initial values of the agent
		finished = false;
		catalogue = new ArrayList<Book>();
		finished_catalogue = new ArrayList<Book>(); 
		bidders = new ArrayList<AID>();

		// Reader for the txt file
		BufferedReader reader;		
		try {
			//For each line in the text file, generate a Book in the catalog
			reader = new BufferedReader(new FileReader(Utils.Path + "auction_catalog.txt"));
			String line = reader.readLine();

			System.out.println("####BOOK CATALOG####");
			while (line != null) {
				// Print the book in the console
				System.out.println(line);
				
				// Split to get the name and the price of each book
				String[] vars = line.split(" - ", 0);
				
				if (vars.length == 2) {
					catalogue.add(new Book(vars[0], Float.parseFloat(vars[1])));
				}
				else {
					Utils.print_msg(getAID().getName(), Utils.RED + "There is an error in the format of the txt file!!! Skipping the line: " + line + Utils.RESET);
				}
				
				// read next line
				line = reader.readLine();
			}

			reader.close();
			System.out.println("####################");
			
		} catch (Exception e) {
			Utils.print_msg(getAID().getName(), "Error at processing the txt file");
			e.printStackTrace();
		}	
			
		// Register the auctioneer so it can be found by the buyers in the directory
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("auctioneer");
		sd.setName("book-auctioneer");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Add the behaviour for the expected messages
		// Done this way to simplify the code but less behaviours with more complex functionality could been implemented.
		addBehaviour(new CatalogueRequestsServer());
		addBehaviour(new OfferResponsesServer());
		addBehaviour(new RejectResponsesServer());
		
		// Every x seconds perform an auction step
		addBehaviour(new TickerBehaviour(this, timer) {
			// Variable to continue with auction even if an agent have not responded
			int wait_response;
			
			// Method executed every x time. Supress unchecked to avoid warning of hash table inside messages
			@SuppressWarnings("unchecked")
			protected void onTick() {
				// If the auction has already finish, we are just waiting for the user to close the gui
				// Note: We evaluate just to close the environment from code if this variable is true 
				// but we believe it will be better that the user have manual control of this action
				if (finished == false) {
					try
					{			
						// If no auction start new one
						if (current_book == null && catalogue.isEmpty() == false)
						{
							Utils.print_msg(getAID().getName(), Utils.GREEN + "Starting new auction" + Utils.RESET);
							
							// Set the state of the variables for a new auction
							current_book = catalogue.get(0);
							current_book.Current_Price = current_book.Initial_Price;
							current_best_offer = null;
							current_offers = new ArrayList<Offer>();
							// Start offering to all bidders, they will be removed one by one for this auction when an reject proposal message arrive
							current_bidders = (ArrayList<AID>)bidders.clone();
							wait_response = 0;
							
							// Send message to all bidders informing a new auction has started
							for (int i = 0; i < bidders.size(); ++i) {
								// Create Call For Proposal message
								ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
								cfp.addReceiver(bidders.get(i));
								cfp.setContentObject(current_book);
								
								myAgent.send(cfp);
							}
						}
						// If an auction is in progress
						else if (current_book != null) {
							//The auctioneer will wait for a response from all bidders (5 ticks max)
							if (current_bidders.size() == current_offers.size() || wait_response >= 5)
							{
								// If only one offer have been received, accept it
								if (current_offers.size() == 1) {
									// Verify the new offer is higher than the previous round(this should never happen, but just in case)
									if (current_best_offer != null) {
										if (current_offers.get(0).Price > current_best_offer.Price) {
											current_best_offer = current_offers.get(0);
										}
										else {
											Utils.print_msg(getAID().getName(), Utils.RED + "The new offer is not higher than the one in previous round!!!" + Utils.RESET);
										}
									}
									else {
										current_best_offer = current_offers.get(0);
									}
									
									Utils.print_msg(getAID().getName(), Utils.BLUE + "Only one offer received, accepting" + Utils.RESET);
											
									// Call to the method that handle the purchase
									handle_purchase(myAgent, current_best_offer);
								}
								// If many offers, continue auction
								else if (current_offers.size() > 1) {								
									// If no offer from previous round, assign the first of the new as best offer
									// This way we also verify the new offers are higher than the previous round (this should never happen, but just in case)
									if (current_best_offer == null) {
										current_best_offer = new Offer(current_offers.get(0).Bidder, current_offers.get(0).Price);
									}
									
									// Iterate all the offers and pick the best one
									for (int i = 0; i < current_offers.size(); ++i) {
										if (current_offers.get(i).Price > current_best_offer.Price) {
											current_best_offer.Price = current_offers.get(i).Price;
											current_best_offer.Bidder = current_offers.get(i).Bidder;
										}
									}
									current_book.Current_Price = current_best_offer.Price;
									current_offers = new ArrayList<Offer>();
															
									Utils.print_msg(getAID().getName(), Utils.BLUE + "Multiple offer received, informing bidders" + Utils.RESET);
									
									// Informt to all the bidders that remain in the auction about the new highest price
									for (int i = 0; i < current_bidders.size(); ++i) {
										ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
										cfp.addReceiver(current_bidders.get(i));
										cfp.setContentObject(current_book);
										
										myAgent.send(cfp);
									}
								}
								// If there are not new offers
								else if (current_best_offer != null) {
									Utils.print_msg(getAID().getName(), Utils.BLUE + "No new offers, accepting previous best" + Utils.RESET);
									
									// Call to the method that handle the purchase
									handle_purchase(myAgent, current_best_offer);
								}
								// If no offers have been recieved for the auction (This should never happen)
								else {
									Utils.print_msg(getAID().getName(), Utils.RED + "No offers for " + current_book.Name + Utils.RESET);
									
									// Clear the variables and move to the next auction
									current_book.Current_Price = (float)0;
									current_book.Buyer_Name = "NA";
									catalogue.remove(0);
									finished_catalogue.add(current_book);
									current_book = null;
								}
							}
							else {
								Utils.print_msg(getAID().getName(), Utils.RED + "Some bidders have not make and offer or reject the proposal!!!" + Utils.RESET);
								wait_response = wait_response + 1;
							}
						}	
						// If the auction finished, tell all the bidders so they can make they own finish actions.
						else {
							Utils.print_msg(getAID().getName(), Utils.GREEN + "No more books to sell, auction finished!!!" + Utils.RESET);
							
							// Send a message to all the bidders informing the auction has finish
							for (int i = 0; i < bidders.size(); ++i) {
								//send message requesting action
								ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
								msg.addReceiver(bidders.get(i));
								
								myAgent.send(msg);
							}
							
							//Save in a file the result of the auction and show it on console
							try {
								float profit = 0;
								String file_name = getAID().getName().split("@")[0];
								
								FileWriter myWriter = new FileWriter(Utils.Path + file_name + ".txt");
								myWriter.write("BOOK NAME - INITIAL PRICE - FINAL PRICE - BUYER");
								Utils.print_msg(getAID().getName(), "BOOK NAME - INITIAL PRICE - FINAL PRICE - BUYER");
								for (int i = 0; i < finished_catalogue.size(); ++i) {
								  myWriter.write(System.lineSeparator());
								  String log = finished_catalogue.get(i).Name + " - " + finished_catalogue.get(i).Initial_Price  
										  + " - " + finished_catalogue.get(i).Current_Price
										  + " - " + finished_catalogue.get(i).Buyer_Name.split("@")[0];
								  myWriter.write(log);
								  Utils.print_msg(getAID().getName(), Utils.GREEN + log  + Utils.RESET);
								  profit = profit + (finished_catalogue.get(i).Current_Price - finished_catalogue.get(i).Initial_Price);
								}
								
								myWriter.write(System.lineSeparator() + System.lineSeparator());
								myWriter.write("Final profit of auction: " + profit);
								myWriter.close();
								
								Utils.print_msg(getAID().getName(), Utils.GREEN + "Final profit: " + profit + Utils.RESET);
						    } 
							catch (IOException e) {
								Utils.print_msg(getAID().getName(), "Error finishing auction");
								e.printStackTrace();
						    }
							
							finished = true;
							Utils.print_msg(getAID().getName(), Utils.RED + "Auction finished. All the agents have been informed. Shut down manually the Agent Platform from the management gui to terminate." + Utils.RESET);
						}
					}
					catch (Exception ex) {
						Utils.print_msg(getAID().getName(), Utils.RED + "Error at processing the auction" + Utils.RESET);
						ex.printStackTrace();
					}
				}
			}
		});
	}

	/// Function that handle the purchase functionality
	private void handle_purchase(Agent agent, Offer offer) {
		try {
			// Change the state of the variables
			current_book.Buyer_Name = offer.Bidder.getName();
			current_book.Current_Price = offer.Price;
			catalogue.remove(0);
			finished_catalogue.add(current_book);
			
			Utils.print_msg(getAID().getName(), Utils.GREEN + current_book.Name 
					+ " selled to " + current_book.Buyer_Name
					+ " at " + current_book.Current_Price
					+ Utils.RESET);
			
			// Inform the bidder that has win the auction
			ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
			msg.addReceiver(offer.Bidder);
			msg.setContentObject(current_book);
			
			agent.send(msg);
			
			current_book = null;
			current_best_offer = null;
		}
		catch (Exception ex) {
			Utils.print_msg(getAID().getName(), "Error at processing catalogue request file");
			ex.printStackTrace();
		}
	};
	
	// Agent is been close. Inform in the console and clean-up the registration
	protected void takeDown() {		
		Utils.print_msg(getAID().getName(), "Closing Auction agent " + getAID().getName() + ".");
		
		// Unregister the service to allow future registrations
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	///Behaviour for the Call For Proposal message. It sends the catalog to any bidder that ask for it and register it in the list to inform about auctions
	private class CatalogueRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// Create a reply to the message
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				
				Utils.print_msg(getAID().getName(), Utils.BLUE + "Catalogue request recieved from:" + msg.getSender().getName() + Utils.RESET);
				
				try {
					// Make a copy of the remaining catalog to send (because if auction in process, we need to modify the list)
					@SuppressWarnings("unchecked")
					ArrayList<Book> catalogue_copy = (ArrayList<Book>)catalogue.clone();
					
					// If an auction is in progress
					if (current_book != null)
					{
						Utils.print_msg(getAID().getName(), "Auction in progress: " + catalogue.get(0).Name);
						// Update the current price for this book.
						// Note: the arquitecture of the solution will prevent that the bidder agent push for this book
						// So this item is removed from the catalog, but other option will be just put the current price 
						// and modifiy the agent to be aware of this auction
						//catalog.get(0).Initial_Price = current_book.Current_Price;
						catalogue_copy.remove(0);
					}
					else {
						Utils.print_msg(getAID().getName(), "Passing catalogue. No auctions in progress");
					}
					
					reply.setContentObject(catalogue_copy);
					
					// Register the bidder in the list
					AID bidder = msg.getSender();
					if (bidders.contains(bidder)) {
						Utils.print_msg(getAID().getName(), "Bidder previously registered");
					}
					else {
						bidders.add(bidder);
						
						Utils.print_msg(getAID().getName(), "Bidder registered");
					}
							
				} catch (Exception e) {
					Utils.print_msg(getAID().getName(), "Error at processing catalogue request file");
					e.printStackTrace();
				}

				myAgent.send(reply);
			}
			else {
				// Blocks this behaviour, is not needed for now
				block();
			}
		}
	}
	
	///Behaviour for the Propose message. Add the offers to the list of current offers
	private class OfferResponsesServer extends CyclicBehaviour {
		public void action() {
			try
			{
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					// Get the price offered by the bidder
					Float price = (Float)msg.getContentObject();
					
					Utils.print_msg(getAID().getName(), "Offer of " + price + " received from " + msg.getSender().getName());
					
					current_offers.add(new Offer(msg.getSender(), price));
				}
				else {
					// Blocks this behaviour, is not needed for now
					block();
				}
			}
			catch (Exception e) {
				Utils.print_msg(getAID().getName(), "Error at processing offer");
				e.printStackTrace();
			}
		}
	}
	
	///Behaviour for the Reject Proposal message. If offer reject, remove the bidder from the interest in this auction.
	private class RejectResponsesServer extends CyclicBehaviour {
		public void action() {
			try
			{
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
					Utils.print_msg(getAID().getName(), Utils.BLUE + "Reject received from " + msg.getSender().getName() + Utils.RESET);
					
					// Remove the bidder from this auction.
					current_bidders.remove(msg.getSender());
				}
				else {
					// Blocks this behaviour, is not needed for now
					block();
				}
			}
			catch (Exception e) {
				Utils.print_msg(getAID().getName(), "Error at processing offer");
				e.printStackTrace();
			}
		}
	}
}
