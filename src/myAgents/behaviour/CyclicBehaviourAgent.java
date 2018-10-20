package myAgents.behaviour;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

public class CyclicBehaviourAgent extends Agent {

	protected void setup(){
		System.out.println("Hello I am: " + getLocalName());
		System.out.println("This is my AID: " + getAID());
		
		//Killing my agent
		//doDelete();
		
		addBehaviour(new MyCyclic());
	}
	
	protected void takeDown(){
		System.out.println("Damn!... you just kill me");
	}

	private class MyCyclic extends CyclicBehaviour {
		public void action() {
			System.out.println("CyclicBehaviour executed!");
		}

		public int onEnd(){
			myAgent.doDelete();
			return super.onEnd();
		}
	}

}
