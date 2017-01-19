package practica3;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;

/**
 * Clase principal que contiene el main del programa
 *
 * @author Samuel Peregrina Morillas
 * @author Luis Gallego Quero
 * @version 1.0
 */
public class main {
  private static final String SERVERPATH = "isg2.ugr.es";
  private static final int PORT = 6000;
  public static final int MAPA = 3;
  public static void main(String[] args){
    Controlador control = null;
    GugelVehicle[] vehiculos = new GugelVehicle[4];
    String mundo = "map"+MAPA;
    System.out.println("Iniciado Programa.");
    AgentsConnection.connect(SERVERPATH,PORT, "Haldus", "Esquivel", "Pegaso", false);
    try {
        //El agentID ahora es Controlador, no GugelCar.
        control = new Controlador(new AgentID("Controlador_"), mundo);
        for(int i=0; i < vehiculos.length; i++){
            vehiculos[i] = new GugelVehicle(new AgentID("Vehiculo" + i));
            vehiculos[i].start();
        }
        control.start();
    } catch (Exception e) { 
      System.err.println("Error al crear los agentes");
      System.err.println(e);
      System.exit(1);
    }    
  }
}
