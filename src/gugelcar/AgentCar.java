package gugelcar;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 * Esta clase contiene el agente que realizará toda la funcionalidad
 *
 * @author Samuel Peregrina
 * @version 1.0
 */
public class AgentCar extends SingleAgent {
    // Diferentes estados del bot
    private final int LOGIN = 0;
    private final int RECIBIR_DATOS = 1;
    private final int ACCION = 2;
    private final int LOGOUT = 3;
    
    private ACLMessage msjSalida, msjEntrada;
    private boolean login, fin;
    private int estadoActual, cont;
    
  public AgentCar(AgentID aid) throws Exception {
    super(aid);
  }

  /**
   *  Método que ejecuta el agente al iniciar su ciclo de vida
   */
  @Override
  public void init() {
      System.out.println("Iniciando GugelCar.");
      estadoActual = LOGIN;
      msjSalida = null;
      msjEntrada = null;
      fin = false;
      //cont = 0;
  }

  /**
   * Método que ejecuta el agente tras el init()
   */
  @Override
  public void execute() {
     System.out.println("Agente activo.");     
     while(!fin) {
         if(estadoActual == 0){
             realizarLogin();
         } else if(estadoActual == 1) {
             resultadoAccion();
             estadoActual = LOGOUT;
         } else if(estadoActual == 2) {
             //cont++;
             realizarAccion(JSON.realizarAccion("moveS"));
             
         } else if(estadoActual == 3) {
             realizarAccion(JSON.realizarAccion("logout"));
             resultadoAccion();
             fin = true;
         }
     }
     
  }

  /**
   * Método que ejecuta el agente antes de finalizar su ciclo de vida
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
      estadoActual = RECIBIR_DATOS;
  }
  
  private void resultadoAccion() {
      System.out.println("Recibiendo respuesta.");
      boolean resultado = true;
      for(int i=0; i<4 && resultado; i++) {
          try {
              msjEntrada = receiveACLMessage();
              //System.out.println("Respuesta recibida." + msjEntrada.getContent());     
              if(login) {
                  System.out.println("ResultadoLogin: ");
                  resultado = JSON.resultadoLogin(msjEntrada.getContent());
                  login = false;
                  //estadoActual = ACCION;
              } else {
                  resultado = JSON.resultadoAccion(msjEntrada.getContent());
              }                 
              
          } catch (InterruptedException ex) {
              System.err.println("Error de comunicación");
          }
      }
   
  }
}
