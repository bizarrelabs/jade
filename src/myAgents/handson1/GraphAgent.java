/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel 
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02 
 * Centro Universitario de Ciencias Exactas e Ingeniería
 * División de Electrónica y Computación
 */

package myAgents.handson1;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class GraphAgent extends Agent {

    private String opc, tmp;
    private Vertex origin;
    private Vertex destination;
    private Vertex current;
    private Graph graph;
    private ArrayList<Vertex> queue;
    private boolean finished = false;

    protected void setup() {
        graph = new Graph();
        queue = graph.getPriorityQueue();
        System.out.println("*** SEARCH ALGORITHMS ***\n");
        addBehaviour(new Menu());
    }

    protected void takeDown() {
        System.out.println("GraphAgent: " + getAID().getName() + " terminating.");
    }

    private class DFS extends OneShotBehaviour {

        public void action() {
            depthFirstSearchRecursive(origin, null);
            if (destination.getPredecessor() != null) {
                graph.printPathDFS(destination);
            } else {
                System.out.println("\n(!)The vertex (" + destination.getID() + ") is inaccessible from (" + origin.getID() + ")");
                finished = true;
                return;
            }

        }

        private void depthFirstSearchRecursive(Vertex current, Vertex o) {
            if (finished == true){
                return;
            }
            if (current.isVisited() == false) {
                //System.out.println("Current: " + current.getID());
                current.setVisited(true);
                graph.addStack(current);
                current.setPredecessor(o);

                if (current == destination) {
                    finished = true;
                } else {
                    current.getEdges().forEach((edge) -> {
                        depthFirstSearchRecursive(edge.getDestination(), current);
                    });
                }
            }
        }
    }

    private class Dijkstra extends CyclicBehaviour {

        private int newWeight;

        public void action() {
            if (finished == true) {
                block();
                return;
            }
            //System.out.println("\nVuelta Dijkastra\n");

            if (queue.isEmpty()) {
                System.out.println("\n(!)The vertex (" + destination.getID() + ") is inaccessible from (" + origin.getID() + ")");
                finished = true;
                return;
            }
            current = queue.remove(0);

            if (current == destination) {
                graph.printPathDijkstra(destination);
                finished = true;

            } else {

                //System.out.println("Current: " + current.getID());
                current.getEdges().forEach((edge) -> {
                    newWeight = current.getWeight() + edge.getWeight();
                    if (newWeight < edge.getDestination().getWeight()) {
                        edge.getDestination().setWeight(newWeight);
                        edge.getDestination().setPredecessor(current);
                        if (!graph.existVertexQueue(edge.getDestination())) {
                            queue.add(edge.getDestination());
                        }
                    }
                });
                //System.out.println("(?)Queue:");
                //graph.printQueue();
                Collections.sort(queue, (Vertex v1, Vertex v2) -> new Integer(v1.getWeight()).compareTo(v2.getWeight()));
                //Collections.sort(queue, (Vertex v1, Vertex v2) -> new Integer(v1.getWeight()).compareTo(v2.getWeight()));
                //System.out.println("(!)Queue:");
                //graph.printQueue();
            }

        }
    }

    private class Menu extends Behaviour {

        private boolean data = false;

        public void action() {
            while (data == false) {
                System.out.println("\n1) Depth First Search (OneShotBehaviourAgent)."
                        + "\n2) Dijkastra (CyclicBehaviourAgent)."
                        + "\n0) Exit");
                System.out.print("\nEnter an option: ");
                Scanner buffer = new Scanner(System.in);
                block();
                opc = buffer.nextLine();
                //System.out.println("Opcion ingresada: " + opc);
                graph.printGraph();
                System.out.print("\nEnter origin vertex: ");
                tmp = buffer.nextLine();
                origin = graph.searchIndex(tmp);
                System.out.print("\nEnter destination vertex: ");
                tmp = buffer.nextLine();
                destination = graph.searchIndex(tmp);

                if (origin != null && destination != null) {
                    if (origin == destination){
                        System.out.println("\n(!)The origin vertex  is equals to destination vertex, please choose other");
                    } else{
                        data = true;
                    }   
                } else {
                    System.out.print("\nChoose a valid option: ");
                }

            }

            switch (opc) {
                case "1":
                    System.out.println("\nDepth First Search (OneShotBehaviourAgent).");

                    addBehaviour(new DFS());
                    block();
                    break;
                case "2":
                    System.out.println("\nDijkastra (CyclicBehaviourAgent).");
                    origin.setWeight(0);

                    origin.getEdges().forEach((edge) -> {
                        edge.getDestination().setWeight(edge.getWeight());
                        edge.getDestination().setPredecessor(origin);
                        if (edge.getDestination() == destination) {
                            graph.printPathDijkstra(destination);
                            finished = true;
                            return;
                        }
                        queue.add(edge.getDestination());
                    });
                    Collections.sort(queue, (Vertex v1, Vertex v2) -> new Integer(v1.getWeight()).compareTo(v2.getWeight()));
                    /*System.out.println("Current: " + origin.getID());
                    System.out.println("Queue:");
                    graph.printQueue();*/
                    addBehaviour(new Dijkstra());
                    block();
                    break;
                case "0":
                    finished = true;
                    break;
                default:
                    System.out.println("(!)The option chosen is not valid");
            }
        }

        public boolean done() {
            return finished;
        }

        public int onEnd() {
            myAgent.doDelete();
            return super.onEnd();
        }
    }

}
