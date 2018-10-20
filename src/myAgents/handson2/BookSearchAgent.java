/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel 
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02 
 * Centro Universitario de Ciencias Exactas e Ingeniería
 * División de Electrónica y Computación
 */

package myAgents.handson2;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BookSearchAgent extends Agent {
	// The title of the book to buy
	private String title;
	// The list of known seller agents
	private AID[] sellerAgents;
	// Put agent initializations here
	public static AID bestSeller;
	public static ACLMessage replyUser;
	
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hallo! BookSearchAgent: "+getAID().getName()+" is ready.");

		
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-searching");
		sd.setName("sql-services");
		sd.addLanguages("English");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add a TickerBehaviour that schedules a request to seller agents every minute
		addBehaviour(new CyclicBehaviour() {
			public void action() {
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
				ACLMessage msg = myAgent.receive(mt);
				if (msg != null) {
				// CFP Message received. Process it
					title = msg.getContent();
					replyUser = msg.createReply();

					System.out.println(getAID().getName()+" Trying to buy "+title);
					// Update the list of seller agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("amazon-sqldatabase-brokering");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Found the following seller agents:");
						sellerAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							sellerAgents[i] = result[i].getName();
							System.out.println(sellerAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					//RequestPerformer request = new RequestPerformer();
					// Perform the request
					myAgent.addBehaviour(new RequestPerformer());
				}
				else {
					block();
				}
			}
		} 
		);

	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("BookSearchAgent "+getAID().getName()+" terminating.");
	}
	
	private class RequestPerformer extends Behaviour {
		//private AID bestSeller; // The agent who provides the best offer 
		private float bestPrice;  // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				} 
				cfp.setContent(title);
				cfp.setConversationId("amazon-sqldatabase-brokering");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("amazon-sqldatabase-brokering"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						float price = Float.parseFloat(reply.getContent());
						if (bestSeller == null || price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						// We received all replies
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(title);
				order.setConversationId("amazon-sqldatabase-brokering");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("amazon-sqldatabase-brokering"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println(title+" successfully purchased from agent "+reply.getSender().getName());
						System.out.println("Price = "+bestPrice);
						
						myAgent.doDelete();
					}
					else {
						System.out.println("Attempt failed: requested book already sold.");
					}

					step = 4;
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			
			if (step == 2 && bestSeller == null) {
				System.out.println("Attempt failed: "+title+" not available for sale");
			} 

			if (((step == 2 && bestSeller == null) || step == 4)){
				replyUser.setPerformative(ACLMessage.INFORM);
				if (bestSeller != null){
					replyUser.setContent(bestSeller.getName());	
				} else {
					replyUser.setContent("");	
				} 
				myAgent.send(replyUser);
			}
			return false;
		}
	}  // End of inner class RequestPerformer
}
