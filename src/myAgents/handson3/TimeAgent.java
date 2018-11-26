/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02
 * Centro Universitario de Ciencias Exactas e Ingeniería
 * División de Electrónica y Computación
 */

package myAgents.handson3;

import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.core.behaviours.TickerBehaviour;
public class TimeAgent extends Agent {

  protected void setup() {
    System.out.println("Agent "+getLocalName()+" started.");

    // Add the TickerBehaviour (period 1 sec)
    addBehaviour(new TickerBehaviour(this, 1000) {
      protected void onTick() {
        System.out.println("Agent "+myAgent.getLocalName()+": tick="+getTickCount());
      }
    });

    addBehaviour(new WakerBehaviour(this, 10000) {
      protected void handleElapsedTimeout() {
        System.out.println("Agent "+myAgent.getLocalName()+": It's wakeup-time. Bye...");
        myAgent.doDelete();
      }
    });
  }
}
