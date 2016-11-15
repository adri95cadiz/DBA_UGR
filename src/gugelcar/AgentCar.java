package gugelcar;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 * Esta clase contiene el agente que realizará toda la funcionalidad
 *
 * @author Samuel Peregrina
 * @author Luis Gallego
 * @version 1.0
 */

public class AgentCar extends SingleAgent {
    // Diferentes estados del bot
    private final int LOGIN = 0;
    private final int RECIBIR_DATOS = 1;
    private final int ACCION = 2; //Mover
    private final int REPOSTAR = 3;
    private final int FINAL = 4; //Logout
    
    private ACLMessage msjSalida, msjEntrada;
    private boolean login;
    private boolean fin;
    private int estadoActual;
    private int nivelBateria=0;
    private int[] datosGPS = new int[2];
    private int[][] datosRadar = new int[5][5];
    private float[][] datosScanner = new float[5][5];
    
    private int cont;
    
  public AgentCar(AgentID aid) throws Exception {
    super(aid);
  }

  /**
   * Inicializamos variables
   * @author Luis Gallego
   */
  @Override
  public void init() {
      System.out.println("Iniciando GugelCar.");
      estadoActual = LOGIN;
      msjSalida = null;
      msjEntrada = null;
      fin = false;
      cont = 0;
  }

  /**
   * Método que ejecuta el agente donde controlamos los estados
   * @author Luis Gallego
   */
  @Override
  public void execute() {
     System.out.println("Agente activo.");     
     while(!fin) {
         switch (estadoActual) {
             case LOGIN:
                 realizarLogin();
                 break;
             case RECIBIR_DATOS:
                 resultadoAccion();
                 break;
             case ACCION:
                 mover();
                 break;
             case REPOSTAR:
                 repostar();
                 break;
             case FINAL:
                 objetivoEncontrado();                 
                 fin = true;
                 break;
         }
     }
     
  }

  /**
   * Método con el que se cierra sesión.
   */
  @Override
  public void finalize() {
    System.out.println("Gugelcar("+ this.getName() +") Terminando");
    super.finalize();
  }
  
  private void realizarLogin() {
      System.out.println("Enviando login.");
      realizarAccion(JSON.realizarLogin());
      login = true;
      System.out.println("Pasamos a recibir datos login");
      estadoActual = RECIBIR_DATOS;
  }
  
  private void realizarAccion(String accion) {
      System.out.println("Enviando accion " + accion + " al servidor");
      msjSalida = new ACLMessage();
      msjSalida.setSender(this.getAid());
      msjSalida.setReceiver(new AgentID("Haldus"));
      msjSalida.setContent(accion);
      this.send(msjSalida);
      System.out.println("Accion enviada.");
      //estadoActual = RECIBIR_DATOS;
  }
  
  private void resultadoAccion() {
      System.out.println("Recibiendo respuesta.");
      boolean resultado = true;
      for(int i=0; i<4 && resultado; i++) {
          try {
              //System.out.println("Recibiendo respuesta2.");
              msjEntrada = receiveACLMessage();
              System.out.println("Respuesta recibida. " + msjEntrada.getContent());     
             if(msjEntrada.getContent().contains("scanner")) {
                  datosScanner = JSON.leerScanner(msjEntrada.getContent());
              } else if(msjEntrada.getContent().contains("radar")) {
                  datosRadar = JSON.leerRadar(msjEntrada.getContent());
              } else if(msjEntrada.getContent().contains("gps")) {
                  datosGPS = JSON.leerGPS(msjEntrada.getContent());
              } else if(login) {
                  System.out.println("ResultadoLogin: ");
                  resultado = JSON.resultadoLogin(msjEntrada.getContent());
                  login = false;
              } else {
                  resultado = JSON.resultadoAccion(msjEntrada.getContent());
              }                 
              
          } catch (InterruptedException ex) {
              System.err.println("Error de comunicación");
          }
      }   
      
      if(!resultado) {
          System.out.println("La conexión ha fallado.");
          estadoActual = FINAL;
      } else if(datosRadar[2][2]==2) {
          estadoActual = FINAL;
      } else if(nivelBateria < 5) {
          estadoActual = REPOSTAR;
      } else {
          estadoActual = ACCION;
      }
  }
  
  private void mover() {
      if(cont==100){
          estadoActual = FINAL;
      } else {
          cont++;
          nivelBateria--;
          realizarAccion(JSON.realizarAccion("moveSW"));
          estadoActual = RECIBIR_DATOS;
      }
  }
  
  /**
   * Estado repostar.
   * @author Luis Gallego
   */
  private void repostar() {
      System.out.println("Respostando.");
      realizarAccion(JSON.realizarAccion("refuel"));
      nivelBateria = 100;
      estadoActual = RECIBIR_DATOS;
  }
  
  /**
   * Estado final.
   * @author Luis Gallego
   */
  private void objetivoEncontrado() {
      System.out.println("Objetivo encontrado.");
      realizarAccion(JSON.realizarAccion("logout"));
      resultadoAccion();
      fin = true;      
  }
  
}
