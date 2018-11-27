/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02
 * Centro Universitario de Ciencias Exactas e Ingenierías
 * División de Electrónica y Computación
 */
package myAgents.handson4;

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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.io.*;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.util.ShapeUtilities;
import org.rosuda.JRI.Rengine;

public class SLR_Agent extends Agent {

    private static final JsonParser parser = new JsonParser();
    private static BufferedReader stdInput;
    private static JsonElement datos;
    public float[][] array = new float[2][17];

    // Put agent initializations here
    protected void setup() {
        System.out.println("Hallo! SLR-Agent " + getAID().getName() + " is ready.");

        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        dfd.setName(getAID());
        sd.setType("simple-linear-regression");
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
        System.out.println("SLR-Agent " + getAID().getName() + " terminating.");
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

        private String x;
        private String y;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            boolean slr = false;

            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it

                x = y = "c(";

                int i = 0;
                while (i < array[0].length) {
                    x += Float.toString(array[0][i]) + ",";
                    y += Float.toString(array[1][i]) + ",";
                    i++;
                }
                x = x.substring(0, x.length() - 1);
                y = y.substring(0, y.length() - 1);
                x += ")";
                y += ")";
                //System.out.println("x=" + x);
                //System.out.println("y=" + y);

                Rengine engine = new Rengine(new String[]{"--no-save"}, false, null);
                engine.eval("x=" + x);
                engine.eval("y=" + y);
                // Comando lm (linear models)
                engine.eval("regression <- lm(y ~ x)");
                //String result = engine.eval("summary(regression)").asString();
                engine.eval("betas = coef(regression)");
                engine.eval("beta0 = betas[1:1]");
                engine.eval("beta1 = betas[2:2]");

                double beta0 = engine.eval("beta0").asDouble();
                double beta1 = engine.eval("beta1").asDouble();

                System.out.printf("%nResult: ŷ = %.3f + %.3fx%n%n", beta0, beta1);

                engine.eval("values <- data.frame(x = seq(51, 60))");

                double[] result = engine.eval("predict(regression, values)").asDoubleArray();

                String outputFile = "predictions.csv";
                boolean alreadyExists = new File(outputFile).exists();

                if (alreadyExists) {
                    File ArchivoEmpleados = new File(outputFile);
                    ArchivoEmpleados.delete();
                }

                try {

                    CsvWriter csvOutput = new CsvWriter(new FileWriter(outputFile, true), ',');

                    csvOutput.write("y");
                    csvOutput.write("x");
                    csvOutput.write("ŷ");
                    csvOutput.write("x1");
                    csvOutput.endRecord();

                    for (i = 0; i < array[0].length; i++) {
                        csvOutput.write(String.valueOf(array[0][i]));
                        csvOutput.write(String.valueOf(array[1][i]));

                        if (i < result.length) {
                          y = String.valueOf(result[i]);
                          y = y.substring(0, 6);
                          csvOutput.write(y);
                          csvOutput.write(String.valueOf(i + 46));
                        }
                        csvOutput.endRecord();
                    }

                    csvOutput.close();

                    ScatterPlot plot = new ScatterPlot("Simple Linear Regression", array, beta0, beta1);
                    plot.pack();
                    RefineryUtilities.centerFrameOnScreen(plot);
                    plot.setVisible(true);

                    slr = true;

                } catch (IOException e) {
                    e.printStackTrace();
                }

                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                if (slr == true) {
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

    public class ScatterPlot extends ApplicationFrame {

        private float data[][];
        private double beta0, beta1;

        public ScatterPlot(String s, float [][] array, double b0, double b1) {
            super(s);
            data = array;
            beta0 = b0;
            beta1 = b1;
            JPanel jpanel = createDemoPanel();
            jpanel.setPreferredSize(new Dimension(800, 600));
            add(jpanel);
        }

        public JPanel createDemoPanel() {
            JFreeChart jfreechart = ChartFactory.createScatterPlot(
                "Chemical process", // Title
                "X",                //
                "Y",
                dataset(), // data
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

            Shape cross = ShapeUtilities.createDiagonalCross(3, 1);
            XYPlot xyPlot = (XYPlot) jfreechart.getPlot();
            xyPlot.setDomainCrosshairVisible(true);
            xyPlot.setRangeCrosshairVisible(true);
            XYItemRenderer renderer = xyPlot.getRenderer();
            renderer.setSeriesShape(0, cross);
            renderer.setSeriesPaint(0, Color.red);
            //renderer.setSeriesShape(1, cross);
            renderer.setSeriesPaint(1, Color.blue);
            return new ChartPanel(jfreechart);
        }

        private XYDataset dataset() {

            XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
            XYSeries series = new XYSeries("Observations");
            XYSeries rect = new XYSeries("Regression rect");

            float r = 40;
            while(r < 80){
                rect.add(r,(beta0+(r*beta1)));
                r += 0.5;
            }

            for (int i = 0; i < data[0].length; i++){
                series.add(data[0][i],data[1][i]);
            }

            xySeriesCollection.addSeries(series);
            xySeriesCollection.addSeries(rect);
            return xySeriesCollection;
        }

    }

}
