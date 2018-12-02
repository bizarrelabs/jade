/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02
 * Centro Universitario de Ciencias Exactas e Ingenierías
 * División de Electrónica y Computación
 */
package handson7NB;

import com.csvreader.CsvReader;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rosuda.JRI.Rengine;

public class Log_Reg_Agent extends Agent {

    // Put agent initializations here
    protected void setup() {
        System.out.println("Hallo! Logistic-Regression-Agent " + getAID().getName() + " is ready.");

        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        dfd.setName(getAID());
        sd.setType("logistic-regression");
        sd.setName("Machine-Learning");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Add the behaviour serving queries from buyer agents
        addBehaviour(new OfferRequestsServer());

        // Add the behaviour serving purchase orders from buyer agents
        addBehaviour(new PurchaseOrdersServer());
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        System.out.println("Logistic-Regression-Agent " + getAID().getName() + " terminating.");
    }

    private class OfferRequestsServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String file = msg.getContent();
                System.out.println("File: " + file);
                ACLMessage reply = msg.createReply();

                File af = new File(file);
                if (af.isFile()) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("file-available");
                } else {
                    System.out.println("Error E/S!!!");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("file-not-available");
                }

                myAgent.send(reply);

            } else {
                block();
            }
        }

    }  // End of inner class OfferRequestsServer

    private class PurchaseOrdersServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);

            double[] predictions;
            boolean mlr = false;

            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String file = msg.getContent();
                // Metodo de regresion logistica con R
                Rengine engine = new Rengine(new String[]{"--no-save"}, false, null);

                engine.eval("data<-read.csv(file='" + file + "',head=TRUE,sep=',')");
                engine.eval("predict<-read.csv(file='predict.csv',head=TRUE,sep=',')");
                engine.eval("e1<-predict$e1");
                engine.eval("e2<-predict$e2");

                engine.eval("exam_1<-data$exam_1");
                engine.eval("exam_2<-data$exam_2");
                engine.eval("admitted<-data$admitted");

                engine.eval("Model_1<-glm(admitted ~ exam_1 +exam_2, family = binomial('logit'), data=data)");
                engine.eval("summary(Model_1)");

                engine.eval("in_frame<-data.frame(exam_1=e1,exam_2=e2)");
                predictions = engine.eval("predict(Model_1,in_frame, type='response')").asDoubleArray();

                //predictions = new double[10];

                try {

                    CsvReader importFile = new CsvReader("predict.csv");
                    importFile.readHeaders();
                    int i = 0;
                    System.out.println("Predictions: ");
                    while (importFile.readRecord()) {
                        String e1 = importFile.get("e1");
                        String e2 = importFile.get("e2");
                        String admitted = Double.toString(predictions[i]*100);
                        admitted = admitted.substring(0, 5);
                        System.out.println("E1: " + e1 + ", E2: " + e2 + ", Admitted?: " + admitted);
                        i++;
                    }

                    importFile.close();

                } catch (IOException e) {

                }

                mlr = true;

                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (mlr == true) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("Regression OK, created for " + msg.getSender().getName());
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Regression not available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }

    }  // End of inner class OfferRequestsServer

}
