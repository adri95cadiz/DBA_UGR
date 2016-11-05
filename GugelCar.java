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
  private final String SERVERPATH = "localhost";
  private final int PORT = 5672;

  public static void main(String[] args) {
    AgentCar Car = null;

    AgentsConnection.connect(SERVERPATH, PORT, );
    try {
      Car = new AgentCar(new AgentID("GugelCar"));
    } catch (Exception e) {
      System.err.println("Error al crear los agentes");
      System.exit(1);
    }
    Car.start();
  }
}
