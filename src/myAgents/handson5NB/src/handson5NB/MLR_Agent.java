/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02
 * Centro Universitario de Ciencias Exactas e Ingenierías
 * División de Electrónica y Computación
 */
package handson5NB;

import com.csvreader.CsvWriter;
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

public class MLR_Agent extends Agent {

    private static final JsonParser parser = new JsonParser();
    private static BufferedReader stdInput;
    private static JsonElement datos;
    public float[][] array = new float[3][17];

    // Put agent initializations here
    protected void setup() {
        System.out.println("Hallo! MLR-Agent " + getAID().getName() + " is ready.");

        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        dfd.setName(getAID());
        sd.setType("multiple-linear-regression");
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
        System.out.println("MLR-Agent " + getAID().getName() + " terminating.");
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
                    //i++;
                }
            } else if (elemento.isJsonNull()) {
                System.out.println("Es NULL");
            } else {
                System.out.println("Es otra cosa");
            }
        }
    }  // End of inner class OfferRequestsServer

    private class PurchaseOrdersServer extends CyclicBehaviour {

        private String x1, x2;
        private String y;
        private int i;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            boolean mlr = false;

            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it

                x1 = x2 = y = "c(";

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
                
                Rengine engine = new Rengine(new String[]{"--no-save"}, false, null);
                engine.eval("x1=" + x1);
                engine.eval("x2=" + x2);
                engine.eval("y=" + y);

                // Comando lm (linear models)
                engine.eval("regression <- lm(y ~ x1 + x2)");
                //String result = engine.eval("summary(regression)").asString();
                //System.out.println("Result:" + result);
                engine.eval("betas = coef(regression)");
                engine.eval("beta0 = betas[1:1]");
                engine.eval("beta1 = betas[2:2]");
                engine.eval("beta2 = betas[3:3]");

                double beta0 = engine.eval("beta0").asDouble();
                double beta1 = engine.eval("beta1").asDouble();
                double beta2 = engine.eval("beta2").asDouble();

                System.out.printf("%nResult: ŷ = %.3f + %.3fx1 + %.3fx2 %n%n", beta0, beta1, beta2);

                engine.eval("values <- data.frame(x1 = seq(51, 60), x2 = seq(29.6, 30.5, 0.1))");

                double[] result = engine.eval("predict(regression, values)").asDoubleArray();

                String outputFile = "mlr-predictions.csv";
                boolean alreadyExists = new File(outputFile).exists();

                if (alreadyExists) {
                    File ArchivoEmpleados = new File(outputFile);
                    ArchivoEmpleados.delete();
                }

                try {

                    CsvWriter csvOutput = new CsvWriter(new FileWriter(outputFile, true), ',');

                    csvOutput.write("y");
                    csvOutput.write("x1");
                    csvOutput.write("x2");
                    csvOutput.write("ŷ");
                    csvOutput.write("x_{1}");
                    csvOutput.write("x_{2}");
                    csvOutput.endRecord();

                    final float j = (float) 0.1;

                    for (i = 0; i < array[0].length; i++) {
                        csvOutput.write(String.valueOf(array[2][i]));
                        csvOutput.write(String.valueOf(array[0][i]));
                        csvOutput.write(String.valueOf(array[1][i]));

                        if (i < result.length) {
                            y = String.valueOf(result[i]);
                            y = y.substring(0, 6);
                            csvOutput.write(y);
                            csvOutput.write(String.valueOf(i + 46));
                            String v = String.valueOf(i * j + 29.6);
                            csvOutput.write(v.substring(0, 4));
                        }
                        csvOutput.endRecord();
                    }

                    csvOutput.close();

                    engine.eval("data<-read.csv(file='" + outputFile + "',head=TRUE,sep=',')");

                    engine.eval("library('scatterplot3d')");
                    engine.eval("s3d <- scatterplot3d(x1,x2,y,type = 'h', color = 'blue', angle=55, pch = 16,"
                            + "main='Multiple Linear Regression',"
                            + "xlab = 'X1',"
                            + "ylab = 'X2',"
                            + "zlab = 'Y')");
                    engine.eval("s3d$plane3d(regression)");
                    //engine.eval("s3d$points3d(seq(10, 20, 2), seq(85, 60, -5), seq(60, 10, -10), col = 'red', type = 'h', pch = 8)");

                    mlr = true;

                } catch (IOException e) {
                    e.printStackTrace();
                }

                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (mlr == true) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("Regression OK, file: '" + outputFile + "' created for " + msg.getSender().getName());
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
