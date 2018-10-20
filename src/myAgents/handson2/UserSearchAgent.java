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

public class UserSearchAgent extends Agent {
	// The title of the book to buy
	private String targetBookTitle;
	// The list of known seller agents
	private AID[] searchAgents;

	private static int attempt = 0;
	private static boolean purchase = false;

	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hallo! UserSearchAgent: "+getAID().getName()+" is ready.");

		// Get the title of the book to buy as a start-up argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			targetBookTitle = (String) args[0];
			System.out.println("Target book UserShearch is "+targetBookTitle);


			// Add a TickerBehaviour that schedules a request to seller agents every minute
			addBehaviour(new TickerBehaviour(this, 10000) {
				protected void onTick() {
					attempt++;
					System.out.println("\nAttempt #"+attempt);
					System.out.println("Trying to search "+targetBookTitle);
					// Update the list of seller agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("book-searching");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Found the following search agents:");
						searchAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							searchAgents[i] = result[i].getName();
							//System.out.println(searchAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}
					// Perform the request
					myAgent.addBehaviour(new Request());
					
					if (attempt >= 3){
						System.out.println("\nThe search agent did not find the book");
						doDelete();
					} 

				}
			} );
		}
		else {
			// Make the agent terminate
			System.out.println("No target book title specified");
			doDelete();
		}
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("UserSearchAgent "+getAID().getName()+" terminating.");
	}

	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by Book-buyer agents to request seller 
	   agents the target book.
	 */
	private class Request extends Behaviour {
		private String inform; 
		private int repliesCnt = 0; // The counter of replies from seller agents
		
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {

			switch (step) {
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < searchAgents.length; ++i) {
					cfp.addReceiver(searchAgents[i]);
				} 
				cfp.setContent(targetBookTitle);
				cfp.setConversationId("book-searching");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-searching"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						inform = reply.getContent();
						//System.out.println("INFORM: "+ inform);
						if (inform.length() > 0){
							System.out.println("Se proceso la compra, se compro al agente: " + inform);
							purchase = true;
						} else {
							System.out.println("No se pudo completar la compra!");
						}
					}
					repliesCnt++;
					if (repliesCnt >= searchAgents.length) {
						// We received all replies
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			}
		}

		public boolean done() {
			return purchase;
			//return true;
		}

		public int onEnd(){
			myAgent.doDelete();
			return super.onEnd();
		}
	}  // End of inner class Request
}
