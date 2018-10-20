package myAgents.hello;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;


public class HelloWorldAgent extends Agent {

	protected void setup(){
		System.out.println("Hello I am: " + getLocalName());
		System.out.println("This is my AID: " + getAID());

		//Killing my agent
		doDelete();

		addBehaviour(new MyOneShot());
	}

	protected void takeDown(){
		System.out.println("Damn!... you just kill me");
	}

	private class MyOneShot extends OneShotBehaviour {

		public void action() {
			System.out.println("OneShotBehaviour executed!");
	 	}

		public int onEnd(){
	 		myAgent.doDelete();
	 		return super.onEnd();
	 	}
	}

}
