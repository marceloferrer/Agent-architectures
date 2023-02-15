package AgentArchitectures;

import jade.core.AID;
// Imports for agents
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

// Imports for file reading
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// Imports for generic behaviour
import java.util.*;

// Class in charge of the auction
public class AuctionAgent extends Agent {
	// Constants
	String path = "C:\\Master\\Multi-Agent System\\book_seller\\src\\AgentArchitectures\\auction_catalog.txt";
	
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
	// The catalog of books for sale (maps the title of a book to its price)
	private ArrayList<Book> finish_catalogue;
	
	// Todo agregar comentarios explicativos

	// Put agent initializations here
	protected void setup() {
		Utils.print_msg(getAID().getName(), "Starting Auction agent");
		
		// Create the catalog
		catalogue = new ArrayList<Book>();
		finish_catalogue = new ArrayList<Book>(); 
		bidders = new ArrayList<AID>();

		BufferedReader reader;		
		try {
			reader = new BufferedReader(new FileReader(path));
			String line = reader.readLine();

			System.out.println("####BOOK CATALOG####");
			while (line != null) {
				System.out.println(line);
				
				String[] vars = line.split(" - ", 0);
				
				catalogue.add(new Book(vars[0], Float.parseFloat(vars[1])));
				// read next line
				line = reader.readLine();
			}

			reader.close();
			System.out.println("####################");
		} catch (Exception e) {
			Utils.print_msg(getAID().getName(), "Error at processing the txt file");
			e.printStackTrace();
		}	
			
		// Register the auctioneer so it can be found by the buyers
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
		
		addBehaviour(new CatalogueRequestsServer());
		addBehaviour(new OfferResponsesServer());
		
		// Every 30 seconds perform an auction step
		addBehaviour(new TickerBehaviour(this, 30000) {
			protected void onTick() {
				try
				{
					Utils.print_msg(getAID().getName(), "Tick");
					
					// If no auction start new one
					if (current_book == null && catalogue.isEmpty() == false)
					{
						Utils.print_msg(getAID().getName(), "Starting new auction");
						current_book = catalogue.get(0);
						current_best_offer = null;
						current_offers = new ArrayList<Offer>();
						current_bidders = (ArrayList<AID>) bidders.clone();
						
						// todo ver si puedo mandar uno para todos
						for (int i = 0; i < bidders.size(); ++i) {
							//send message requesting action
							ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
							cfp.addReceiver(bidders.get(i));
							cfp.setContentObject(current_book);
							
							myAgent.send(cfp);
						}
						// todo send message to all bidders
						// todo sends a CFP with the name of the auctioned good and its initial price to all possible bidders
					}
					// If auction
					else if (current_book != null) {
						// If only one offer, accept
						if (current_offers.size() == 1) {
							Utils.print_msg(getAID().getName(), "Only one offer received, accepting");
						}
						// If many offers, continue auction
						else if (current_offers.size() > 1) {
							Utils.print_msg(getAID().getName(), "Only one offer received, accepting");
							
						}
						else if (current_best_offer != null) {
							Utils.print_msg(getAID().getName(), "No new offers, accepting previous best");
						}
						else {
							Utils.print_msg(getAID().getName(), "No offers for " + current_book.Name);
						}
					}	
				}
				catch (Exception ex) {
					Utils.print_msg(getAID().getName(), "Error at processing the the tick");
					ex.printStackTrace();
				}
			}
		});
		
	}

	// Put agent clean-up operations here
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
	
	private class CatalogueRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				ACLMessage reply = msg.createReply();
				
				System.out.println(msg.getSender());

				// The requested book is available for sale. Reply with the price
				reply.setPerformative(ACLMessage.INFORM);
				Utils.print_msg(getAID().getName(), "Catalogue request recieved from:" + msg.getSender().getName());
				
				try {
					//todo including the current price of the one being auctioned at the moment (if that is the case).
					
					Utils.print_msg(getAID().getName(), "First book of catalogue" + catalogue.get(0).Name);
					reply.setContentObject(catalogue);
					
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
				block();
			}
		}
	}
	
	private class OfferResponsesServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				ACLMessage reply = msg.createReply();
				
				System.out.println(msg.getSender());

				// The requested book is available for sale. Reply with the price
				reply.setPerformative(ACLMessage.INFORM);
				Utils.print_msg(getAID().getName(), "Catalogue request recieved from:" + msg.getSender().getName());
				
				try {
					reply.setContentObject(catalogue);
				} catch (Exception e) {
					Utils.print_msg(getAID().getName(), "Error at processing catalogue request file");
					e.printStackTrace();
				}

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}
}
