/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02
 * Centro Universitario de Ciencias Exactas e Ingenierías
 * División de Electrónica y Computación
 */
package handson7NB;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class LR_User extends Agent {
    // The title of the book to buy

    private String regressionType;
    // The list of known seller agents
    private AID[] linearRegresionAgents;

    // Put agent initializations here
    @Override
    protected void setup() {
        // Printout a welcome message
        System.out.println("Hallo! Logistic-Regression-User " + getAID().getName() + " is ready.");

        // Get the title of the book to buy as a start-up argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            //regressionType = ((String) args[0]).toUpperCase();
            regressionType = (String) args[0];

            // Add a TickerBehaviour that schedules a request to seller agents every minute
            addBehaviour(new TickerBehaviour(this, 1000) {
                protected void onTick() {
                    System.out.println("Type of regression: " + regressionType);
                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(regressionType + "-regression");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found the following " + regressionType + "-regression-agents:");
                        linearRegresionAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            linearRegresionAgents[i] = result[i].getName();
                            System.out.println(linearRegresionAgents[i].getName());
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Perform the request
                    myAgent.addBehaviour(new RequestPerformer());
                }
            });
        } else {
            // Make the agent terminate
            System.out.println("No type of regression specified");
            doDelete();
        }
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("SLR-User " + getAID().getName() + " terminating.");
    }

    private class RequestPerformer extends Behaviour {

        private AID bestAgent; // The agent who provides the best offer
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private String file = "Marks.csv";

        public void action() {
            switch (step) {
                case 0:

                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < linearRegresionAgents.length; ++i) {
                        cfp.addReceiver(linearRegresionAgents[i]);
                    }
                    cfp.setContent(file);
                    cfp.setConversationId("logistic-regression");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("logistic-regression"),
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
                            String msg = reply.getContent();
                            bestAgent = reply.getSender();
                            System.out.println("User recibe: " + msg);
                            step = 2;
                        }

                    } else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestAgent);
                    order.setContent(file);
                    order.setConversationId("logistic-regression");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("logistic-regression"),
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
                            System.out.println(regressionType + "-regression successfully done by the agent " + reply.getSender().getName());
                            myAgent.doDelete();
                        } else {
                            System.out.println("Attempt failed: requested book already sold.");
                        }

                        step = 4;
                    } else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestAgent == null) {
                System.out.println("Attempt failed: " + regressionType + " not available for sale");
            }
            return ((step == 2 && bestAgent == null) || step == 4);
        }
    }  // End of inner class RequestPerformer
}
