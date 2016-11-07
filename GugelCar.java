package gugelcar;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;

/**
 * Clase principal que contiene el main del programa
 *
 * @author Samuel Peregrina Morillas
 * @version 1.0
 */
public class GugelCar {
  private static final String SERVERPATH = "isg2.ugr.es";
  private static final int PORT = 6000;

  public static void main(String[] args) throws Exception {
    AgentCar Car = null;

    AgentsConnection.connect(SERVERPATH,PORT, "Haldus", "Esquivel", "Pegaso", false);
    try {
      Car = new AgentCar(new AgentID("GugelCar"));
    } catch (Exception e) {
      System.err.println("Error al crear los agentes");
      System.exit(1);
    }
    Car.start();
  }
}
