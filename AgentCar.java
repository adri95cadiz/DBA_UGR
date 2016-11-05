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
  public AgentCar(AgentID aid) throws Exception {
    super(aid);
  }

  /**
   *  Método que ejecuta el agente al iniciar su ciclo de vida
   */
  @Override
  public void init() {}

  /**
   * Método que ejecuta el agente tras el init()
   */
  @Override
  public void execute() {}

  /**
   * Método que ejecuta el agente antes de finalizar su ciclo de vida
   */
  @Override
  public void finalize() {
    System.out.println("Gugelcar("+ this.getName() +") Terminando");
    super.finalize();
  }
}
