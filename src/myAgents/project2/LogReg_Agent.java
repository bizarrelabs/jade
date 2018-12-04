/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02
 * Centro Universitario de Ciencias Exactas e Ingenierías
 * División de Electrónica y Computación
 */
package myAgents.project2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.io.*;
import org.rosuda.JRI.Rengine;

public class LogReg_Agent extends Agent {

    private static final JsonParser parser = new JsonParser();
    private static BufferedReader stdInput;
    private static JsonElement datos;
    public float[][] array = new float[3][10];
    Rengine engine = new Rengine(new String[]{"--no-save"}, false, null);

    // Put agent initializations here
    protected void setup() {
        System.out.println("Hallo! LogisticRegresion-Agent " + getAID().getName() + " is ready.");

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

                // Leer el archivo JSON
                try {
                    Process p = Runtime.getRuntime().exec(file);
                    stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    JsonElement datos = parser.parse(stdInput);
                    int i = 0;
                    int j = 0;
                    dumpJSONElement(datos, i, j);

                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("file-available");

                } catch (IOException e) {
                    System.out.println("Error E/S: " + e);
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("file-not-available");
                }

                myAgent.send(reply);
            } else {
                block();
            }
        }

        private void dumpJSONElement(JsonElement elemento, int i, int j) {

            if (elemento.isJsonObject()) {
                //System.out.println("Es objeto");
                JsonObject obj = elemento.getAsJsonObject();
                java.util.Set<java.util.Map.Entry<String, JsonElement>> entradas = obj.entrySet();
                java.util.Iterator<java.util.Map.Entry<String, JsonElement>> iter = entradas.iterator();
                while (iter.hasNext()) {
                    java.util.Map.Entry<String, JsonElement> entrada = iter.next();
                    //System.out.println("Clave: " + entrada.getKey());
                    //System.out.println("Valor:");
                    j = 0;
                    dumpJSONElement(entrada.getValue(), i, j);
                    i++;
                }

            } else if (elemento.isJsonArray()) {
                JsonArray array = elemento.getAsJsonArray();
                //System.out.println("Es array. Numero de elementos: " + array.size());
                java.util.Iterator<JsonElement> iter = array.iterator();
                while (iter.hasNext()) {
                    JsonElement entrada = iter.next();
                    dumpJSONElement(entrada, i, j);
                    j++;
                }
            } else if (elemento.isJsonPrimitive()) {
                //System.out.println("Es primitiva");
                JsonPrimitive valor = elemento.getAsJsonPrimitive();
                if (valor.isBoolean()) {
                    //System.out.println("Es booleano: " + valor.getAsBoolean());
                } else if (valor.isNumber()) {
                    //System.out.println("Es numero: " + valor.getAsNumber());
                } else if (valor.isString()) {
                    //System.out.println("Es texto: " + valor.getAsString());
                    //System.out.println("i:" + i + "j:" + j);
                    array[i][j] = Float.parseFloat(valor.getAsString());
                    //System.out.println(array[i][j]);
                    i++;
                }
            } else if (elemento.isJsonNull()) {
                //System.out.println("Es NULL");
            } else {
                //System.out.println("Es otra cosa");
            }
        }
    }  // End of inner class OfferRequestsServer

    private class PurchaseOrdersServer extends CyclicBehaviour {

        private String x1;
        private String x2;
        private String y;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);

            boolean lr = false;

            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it

                String vars = msg.getContent();

                String[] parts = vars.split("-");
                String var1 = parts[0];
                String var2 = parts[1];

                lr = calculate(var1, var2);

                ACLMessage reply = msg.createReply();

                if (lr == true) {
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

        private boolean calculate(String var1, String var2) {
            double predictions[];
            x1 = x2 = y = "c(";

            int i = 0;
            while (i < array[0].length) {
                x1 += Float.toString(array[0][i]) + ",";
                x2 += Float.toString(array[1][i]) + ",";
                y += Float.toString(array[2][i]) + ",";
                i++;
            }
            x1 = x1.substring(0, x1.length() - 1);
            x2 = x2.substring(0, x2.length() - 1);
            y = y.substring(0, y.length() - 1);
            x1 += ")";
            x2 += ")";
            y += ")";

            //System.out.println("x1=" + x1);
            //System.out.println("x2=" + x2);
            //System.out.println("y=" + y);
            engine.eval("x1=" + x1);
            engine.eval("x2=" + x2);
            engine.eval("y=" + y);

            // Comando lm (linear models)
            engine.eval("regression <- glm(y ~ x1 + x2, family = binomial('logit'))");
            //System.out.println(engine.eval("summary(regression)"));

            System.out.println("\n\tVariables to predict: x1=" + var1 + ", x2=" + var2);

            engine.eval("in_frame<-data.frame(x1=" + var1 + ",x2=" + var2 + ")");

            predictions = engine.eval("predict(regression,in_frame, type='response')").asDoubleArray();

            System.out.print("\t");
            System.out.printf("Probability = %.3f",predictions[0]);
            System.out.println("% \n");

            if (predictions != null) {
                return true;
            } else {
                return false;
            }

        }
    }  // End of inner class OfferRequestsServer
}
