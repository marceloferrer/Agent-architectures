package AgentArchitectures;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.UnreachableException;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

// DF = Directory Facilitator

//Class in charge of bidder behavior
public class ReactiveAgent extends Agent{
	// Instance of random class
	Random rand = new Random(); 
	
	// Auctioneer agent
	private AID auctioneer;
	// Hashtable with the interest in each book
	private Hashtable interests;
	// Level of aggressiveness of the bidder
	private int aggressiveness;
	
	// Budget of the bidder
	private float budget;
		
	// Put agent initializations here
	protected void setup() {
		Utils.print_msg(getAID().getName(), "Starting Bidding agent " + getAID().getName() + ".");

	    // Random value between 0 and 10
		aggressiveness = rand.nextInt(11);
		interests = new Hashtable();
		
		// Get the auctioneer
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("auctioneer");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template); 
			
			if (result.length > 0)
			{
				auctioneer = result[0].getName();
				Utils.print_msg(getAID().getName(), "Auctioneer registered");
				
				//send message requesting action
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				cfp.addReceiver(auctioneer);
				cfp.setConversationId("Catalogue-request-" + getAID().getName());
				this.send(cfp);
			}
			else {
				System.out.println("Auctioneer not detected!!!");
			}
		}
		catch (FIPAException fe) {
			Utils.print_msg(getAID().getName(), "Error in starting behavior of Bidder");
			fe.printStackTrace();
		}
		
		addBehaviour(new CatalogueResponseServer());
		addBehaviour(new AuctionResponseServer());
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		Utils.print_msg(getAID().getName(), "Closing Bidder agent " + getAID().getName() + ".");
		
		// Bidder is not registered, so no need to Unregister
	}
	
	///Receives the catalog
	private class CatalogueResponseServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Catalogue response recieved from:" + msg.getSender().getName());
				
				try {
					ArrayList<Book> catalog = (ArrayList<Book>) msg.getContentObject();
					Utils.print_msg(getAID().getName(), "Catalogue recived:" + catalog);
					
					for (int i = 0; i < catalog.size(); ++i) {
						Utils.print_msg(getAID().getName(), i + " Adding " + catalog.get(i).Name);
						// Give a random interest to each book in the catalog
						interests.put(catalog.get(i).Name, rand.nextInt(11));
					}
				} catch (Exception e) {
					Utils.print_msg(getAID().getName(), "Error at processing catalogue response");
					e.printStackTrace();
				}
			}
			else {
				block();
			}
		}
	}
	
	///Receives the auction announcement
	private class AuctionResponseServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println("Auction response recieved from:" + msg.getSender().getName());
				
				try {
					Book book = (Book) msg.getContentObject();
					Utils.print_msg(getAID().getName(), "Book auction:" + book.Name);
				} catch (Exception e) {
					Utils.print_msg(getAID().getName(), "Error at processing catalogue response");
					e.printStackTrace();
				}
			}
			else {
				block();
			}
		}
	}
}