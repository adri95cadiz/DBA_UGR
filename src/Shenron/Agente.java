package Shenron;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 *
 * @author Luis Gallego Quero
 */
public class Agente extends SingleAgent {
    
    private boolean reiniciar;

    public Agente(AgentID aid, boolean reiniciar) throws Exception {
	super(aid);
	this.reiniciar = reiniciar;
    }
    
    private void enviarMensaje(int primitiva, String content) {
	ACLMessage message = new ACLMessage();
	message.setSender(getAid());
	message.setReceiver(new AgentID("Shenron"));
	message.setContent(content);
	message.setPerformative(primitiva);
	send(message);
    }
    
    @Override
    protected void execute() {
        String content = "{\"user\":\"Esquivel\", \"password\":\"Pegaso\"}";
        if (reiniciar) {
	    enviarMensaje(ACLMessage.REQUEST, content);
	} else {
	    enviarMensaje(ACLMessage.QUERY_REF, content);
	}
        
        try {
	    ACLMessage message = receiveACLMessage();
	    System.out.println("\n");
	    if (message.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD) {
		System.out.println("No se ha entendido el mensaje");
	    } else if (message.getPerformativeInt() == ACLMessage.FAILURE) {
		System.out.println("Ha fallado la petición");
	    } else if (message.getPerformativeInt() == ACLMessage.INFORM) {
		System.out.println("Petición correcta. Respuesta:");
		System.out.println(message.getContent());
	    }
	} catch (InterruptedException ex) {
	    System.err.println(ex.toString());
	}        
    }    
}
