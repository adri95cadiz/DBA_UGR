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
    private ACLMessage mensaje;
    
  public AgentCar(AgentID aid) throws Exception {
    super(aid);
  }

  /**
   *  Método que ejecuta el agente al iniciar su ciclo de vida
   */
  @Override
  public void init() {
      System.out.println("Iniciando GugelCar.");
  }

  /**
   * Método que ejecuta el agente tras el init()
   */
  @Override
  public void execute() {
      iniciarLogin();
      resultadoLogin();
  }

  /**
   * Método que ejecuta el agente antes de finalizar su ciclo de vida
   */
  @Override
  public void finalize() {
    System.out.println("Gugelcar("+ this.getName() +") Terminando");
    super.finalize();
  }
  
  private void iniciarLogin() {
      System.out.println("Solicitando login.");
      System.out.println("Enviando login.");
      mensaje = new ACLMessage();
      mensaje.setSender(this.getAid());
      mensaje.setReceiver(new AgentID("Haldus"));
      mensaje.setContent(JSON.realizarLogin());
      this.send(mensaje);
      System.out.println("Login enviado");
  }
  
  private void resultadoLogin() {
      boolean correcto;
      try {
        mensaje = receiveACLMessage();
        correcto = JSON.resultadoLogin(mensaje.getContent());
          if(correcto) {
          System.out.println("Login OK");
          } else {
          System.out.println("Error en el login");
      }
      } catch (InterruptedException ex) {
		System.err.println("Error de comunicación");
        }       
  }
}
