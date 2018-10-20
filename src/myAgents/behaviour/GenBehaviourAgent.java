package myAgents.behaviour;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;

public class GenBehaviourAgent extends Agent {

	protected void setup(){
		System.out.println("Hello I am: " + getLocalName());
		System.out.println("This is my AID: " + getAID());
		
		//Killing my agent
		//doDelete();
		
		addBehaviour(new MyGenBehaviour());
	}
	
	protected void takeDown(){
		System.out.println("Damn!... you just kill me");
	}

	private class MyGenBehaviour extends Behaviour {
	
		public void action() {
			System.out.println("OneShot executed... this time!");
		}
		
		public boolean done(){
			return true;
		}	

		public int onEnd(){
			myAgent.doDelete();
			return super.onEnd();
		}
	}

}
