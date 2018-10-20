/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel 
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02 
 * Centro Universitario de Ciencias Exactas e Ingeniería
 * División de Electrónica y Computación
 */

package myAgents.handson1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;

public class Graph {

    private final ArrayList<Vertex> listVertex;
    private final ArrayList<Vertex> priorityQueue;
    private final ArrayList<Vertex> stack;

    public Graph() {
        this.listVertex = new ArrayList<>();
        this.priorityQueue = new ArrayList<>();
        this.stack = new ArrayList<>();
        uploadFromFile();
    }

    public ArrayList<Vertex> getListVertex() {
        return listVertex;
    }

    public ArrayList<Vertex> getPriorityQueue() {
        return priorityQueue;
    }

    public ArrayList<Vertex> getStack() {
        return stack;
    }

    private void addVertex(String id) {
        listVertex.add(new Vertex(id));
    }
    
    public void addStack(Vertex v) {
        stack.add(0, v);
    }

    public Vertex searchIndex(String e) {
        int pos = 0;
        Vertex vertex;
        Iterator<Vertex> it = listVertex.iterator();

        while (it.hasNext()) {
            vertex = it.next();
            if (vertex.getID().equalsIgnoreCase(e)) {
                return vertex;
            }
            pos++;
        }
        return null;
    }

    public boolean existVertexQueue(Vertex v) {
        Vertex vertex;
        Iterator<Vertex> it = priorityQueue.iterator();

        while (it.hasNext()) {
            vertex = it.next();
            if (vertex == v) {
                return true;
            }
        }
        return false;
    }

    private void uploadFromFile() {
        try {
 
		        FileReader file = new FileReader("src/myAgents/handson1/graph.txt");
            BufferedReader linea = new BufferedReader(file);

            String buffer;
            while ((buffer = linea.readLine()) != null) {
                addVertex(Character.toString(buffer.charAt(0)));
            }
            file.close();

            file = new FileReader("src/myAgents/handson1/graph.txt");
            linea = new BufferedReader(file);
            int weight;
            Vertex verOrigin;
            Vertex verDestination;

            while ((buffer = linea.readLine()) != null) {
                String[] parts = buffer.split(Pattern.quote(","));
                int size = parts.length;
                int i = 1;
                verOrigin = searchIndex(parts[0]);
                if (verOrigin != null) {
                    while (i < size) {
                        weight = Integer.parseInt(parts[i]);
                        verDestination = searchIndex(parts[i + 1]);
                        //destination = searchIndex(parts[i + 1]);
                        i += 2;
                        //verOrigin = listVertex.get(origin);
                        Edge edge = new Edge(weight, verOrigin, verDestination);
                        verOrigin.addEdges(edge);
                    }
                } else {
                    System.out.println("(!) Index not found!!!");
                }

            }
            file.close();
        } //Si se causa un error al leer cae aqui
        catch (IOException e) {
            System.out.println("(!) Error: read to file");
        }
    }

    public void printGraph() {
        Vertex vertex;
        Edge edge;
        Iterator<Vertex> v = listVertex.iterator();

        while (v.hasNext()) {
            vertex = v.next();
            Iterator<Edge> e = vertex.getEdges().iterator();
            System.out.print("\tVertex " + vertex.getID() + " : ");
            while (e.hasNext()) {
                edge = e.next();
                System.out.print("(" + edge.getWeight() + "," + edge.getDestination().getID() + ")");
            }
            System.out.println("");
        }
    }

    public void printQueue() {
        Vertex vertex;
        Edge edge;
        Iterator<Vertex> v = priorityQueue.iterator();

        while (v.hasNext()) {
            vertex = v.next();
            System.out.println("\tVertex: " + vertex.getID() + " Weight: " + vertex.getWeight());
        }
    }

    public void printPathDijkstra(Vertex e) {
        ArrayList<String> travel = new ArrayList<>();
        Vertex vertex = e;
        travel.add(vertex.getID());

        while (vertex.getPredecessor() != null) {
            travel.add(vertex.getPredecessor().getID());
            vertex = vertex.getPredecessor();
        }
        Collections.reverse(travel);
        System.out.print("\nOutput: ");
        int i = 0;
        int size = travel.size();
        while (i < size) {
            System.out.print(travel.get(i) + " ");
            if (i < size - 1) {
                System.out.print("--> ");
            }
            i++;
        }
        System.out.println(" PATH WEIGHT: " + e.getWeight());
    }
    public void printPathDFS(Vertex e) {
        ArrayList<String> travel = new ArrayList<>();
        Vertex vertex = e;
        travel.add(vertex.getID());

        while (vertex.getPredecessor() != null) {
            travel.add(vertex.getPredecessor().getID());
            vertex = vertex.getPredecessor();
        }
        Collections.reverse(travel);
        System.out.print("\nOutput: ");
        int i = 0;
        int size = travel.size();
        while (i < size) {
            System.out.print(travel.get(i) + " ");
            if (i < size - 1) {
                System.out.print("--> ");
            }
            i++;
        }
        //System.out.println(" PATH WEIGHT: " + e.getWeight());
    }
}

class Edge {

    private final int weight;
    private final Vertex origin, destination;

    public Edge(int w, Vertex o, Vertex d) {
        this.weight = w;
        this.origin = o;
        this.destination = d;
    }

    public int getWeight() {
        return weight;
    }

    public Vertex getOrigin() {
        return origin;
    }

    public Vertex getDestination() {
        return destination;
    }

}

class Vertex {

    private final String id;
    private final ArrayList<Edge> listEdge;
    private int weight;
    private Vertex predecessor;
    private boolean visited;

    public Vertex(String id) {
        this.id = id;
        this.listEdge = new ArrayList<>();
        this.weight = 999999999;
        this.visited = false;
    }

    public Vertex(String id, int w) {
        this.id = id;
        this.listEdge = null;
        this.weight = w;
    }

    public String getID() {
        return id;
    }

    public ArrayList<Edge> getEdges() {
        return listEdge;
    }

    public void addEdges(Edge e) {
        this.listEdge.add(e);
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int w) {
        this.weight = w;
    }

    public void setPredecessor(Vertex p) {
        this.predecessor = p;
    }

    public Vertex getPredecessor() {
        return predecessor;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean v) {
        visited = v;
    }

}
