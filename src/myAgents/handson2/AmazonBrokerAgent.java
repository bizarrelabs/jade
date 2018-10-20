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
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.sql.*;

import java.util.*;

public class AmazonBrokerAgent extends Agent {

	public ConenctDB connDB;

	// The catalogue of books for sale (maps the title of a book to its price)
	private Hashtable catalogue;

	// The GUI by means of which the user can add books in the catalogue
	private BookSellerGui myGui;

	// Put agent initializations here
	protected void setup() {
		System.out.println("Hallo! AmazonBrokerAgent: "+getAID().getName()+" is ready.");

		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			String region = (String) args[0];
			String database = "";
			String puerto = "";
			//System.out.println("The region of this agent is: "+region);
			// host, port, database, user, password
			switch (region){
				case "coast":
					database = "AmazonBooksCoastRegion";
					puerto = "3306";
					break;
				case "east":
					database = "AmazonBooksEastRegion";
					puerto = "3307";
					break;
				case "west":
					database = "AmazonBooksWestRegion";
					puerto = "3308";
					break;
				default:
					database = "None";
					break;
			}
			connDB = new ConenctDB ("localhost", puerto, database,"jade", "jadepass");
			connDB.connectDatabase();
			//System.out.println("Precio de Troya: "+ connDB.shearchByName("Troya"));

			// Create the catalogue
			catalogue = new Hashtable();

			// Create and show the GUI
			myGui = new BookSellerGui(this);
			myGui.showGui();

			// Register the book-selling service in the yellow pages
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("amazon-sqldatabase-brokering");
			sd.setName(region+"-region-brokering");
			sd.addLanguages("SQL");
			dfd.addServices(sd);
			try {
				DFService.register(this, dfd);
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}

			// Add the behaviour serving queries from buyer agents
			addBehaviour(new OfferRequestsServer());

			// Add the behaviour serving purchase orders from buyer agents
			addBehaviour(new PurchaseOrdersServer());
		}	else {
			// Make the agent terminate
			System.out.println("No target region specified");
			doDelete();
		}
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
		// Cerrar la base de datos
		connDB.closeDatabase();
		// Close the GUI
		myGui.dispose();
		// Printout a dismissal message
		System.out.println("AmazonBrokerAgent: "+getAID().getName()+" terminating.");
	}

	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
			// CFP Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				//Integer price = (Integer) catalogue.get(title);
				float price = connDB.shearchByName(title);
				if (price > 0) {
					System.out.println("Price: "+price+" : "+getAID().getName());
				// The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price));
				}
				else {
				// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer


	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				int row = connDB.deleteByName(title);

				if (row > 0) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title+" sold to agent "+msg.getSender().getName());
				}
				else {
					// The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	public class ConenctDB {
		private String host;
		private String port;
		private String database;
		private String user;
		private String password;
		private Connection conn = null;

		public ConenctDB(String host, String port, String database, String user, String password){
			this.host = host;
			this.port = port;
			this.database = database;
			this.user = user;
			this.password = password;
		}

		public void connectDatabase() {
			String url = null;
			try {
	            url ="jdbc:mysql://" + host + ":" + port + "/" + database;
	            // Database connect
	            this.conn = DriverManager.getConnection(url,user,password);
	            boolean valid = conn.isValid(50000);
	            //System.out.println(valid ? "DATABASE "+ database +" TEST OK" : "TEST FAIL");
	        } catch (java.sql.SQLException sqle) {
	        	System.out.println("Error connecting to the MySQL database (" + url + "): " + sqle);
	        }
	    }

	    public void closeDatabase(){
	    	try{
	    		if (conn != null) {
	    			conn.close();
	    			System.out.println("Connection close");
	    		}
	    	} catch(java.sql.SQLException ex){
	    		System.out.println("Error closing to the MySQL database:" + ex);
	    	}
	    }

	    public float shearchByName(String targetBookTitle){
	    	if (conn != null){
		    	try{
		    		PreparedStatement query = conn.prepareStatement("SELECT precio FROM Libros WHERE titulo = ?");
					//query.setInt(1, targetBookTitle);
		    		query.setString(1, targetBookTitle);
		    		ResultSet resultado = query.executeQuery();

		    		if (resultado != null){
		    			while(resultado.next()){
		    				return (float) resultado.getInt("precio");
		    			}
		    		}

		    	} catch (java.sql.SQLException ex){
		    		System.out.println("Error al buscar por titulo: " + ex);
		    	}
	    	}
	    	return -1;
	    }

	    public int deleteByName(String targetBookTitle){
	    	if (conn != null){
		    	try{
		    		PreparedStatement query = conn.prepareStatement("DELETE FROM Libros WHERE titulo = ?");
		    		query.setString(1, targetBookTitle);
		    		return query.executeUpdate();

		    	} catch (java.sql.SQLException ex){
		    		System.out.println("Error al buscar por titulo: " + ex);
		    	}
	    	}
	    	return -1;
	    }

	    public void updateCatalogue(final String title, final float price) {

	    	addBehaviour(new OneShotBehaviour() {
		 		public void action() {

		 			if (conn != null){
				    	try{

				    		String sql = "INSERT INTO Libros (id,titulo,autor,puntos,precio) VALUES (?,?,?,?,?)";
				    		PreparedStatement query = conn.prepareStatement(sql);
				    		query.setString(1, null);
				    		query.setString(2, title);
				    		query.setString(3, null);
				    		query.setString(4, null);
				    		query.setFloat(5, price);

				    		if (query.executeUpdate() > 0){
				    			System.out.println(title + " successfully inserted. Price: " + price);
				    		} else {
				    			System.out.println("Could not be inserted into the database!");
				    		}

				    	} catch (java.sql.SQLException ex){
				    		System.out.println("Error inserting book title: " + ex);
				    	}
			    	}
		 		}
		 	} );


	    }



	}

}
