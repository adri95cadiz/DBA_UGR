package practica3;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.HashMap;

/**
 *
 * @author Luis Gallego Quero
 */
public class GugelVehicle extends SingleAgent {
    
    private final String NOMBRE_CONTROLADOR = "Controlador_";
    private final String NOMBRE_SERVIDOR = "Haldus";
    private ACLMessage msjEntrada, msjSalida;
    String receptor;
    boolean fin;
    private String conversationID;
    //boolean primero;
    String reply;
    //private HashMap<String, String> reply;
    
    public GugelVehicle(AgentID id) throws Exception{
        super(id);
    }
    
    public void init(){
        System.out.println("Iniciandose vehiculo " + getName());
        msjEntrada = null;
        msjSalida = null;
        fin = false;
        conversationID = null;
        /*
        primero = true;        
        reply = new HashMap<>();   // Creo que esto no hace falta
        reply.put("Vehiculo0", null);
        reply.put("Vehiculo1", null);
        reply.put("Vehiculo2", null);
        reply.put("Vehiculo3", null); */
    }   
    
    public void execute(){
        while(!fin){
            try {
                msjEntrada = receiveACLMessage();
                System.out.println("\n"+getName() + " ha recibido: " + msjEntrada.getContent() + " ConvID: " + msjEntrada.getConversationId() + " Reply: " + msjEntrada.getReplyWith());
                conversationID = msjEntrada.getConversationId();
                if(!msjEntrada.getReplyWith().isEmpty()) {
                    reply = msjEntrada.getReplyWith();
                    //reply.put(getName(), msjEntrada.getReplyWith());   
                }                             
                if(msjEntrada.getPerformativeInt() == ACLMessage.CANCEL){
                    fin = true; 
                } else {
                    if(msjEntrada.getSender().name.equals(NOMBRE_SERVIDOR)){
                        receptor = NOMBRE_CONTROLADOR;
                    } else {
                        receptor = NOMBRE_SERVIDOR;
                    }
                    enviar(receptor, msjEntrada.getPerformativeInt(), msjEntrada.getContent());
                }
            } catch (InterruptedException ex) {
                System.err.println("Agente, error de comunicación");
            }
        }        
    }
    
    public void finalize(){
        super.finalize();
    }
    
    private void enviar(String receptor, int performativa, String contenido) {
        System.out.println("\nEn el enviar mensaje del vehiculo \n");
        msjSalida = new ACLMessage();
        msjSalida.setSender(this.getAid());
        msjSalida.setReceiver(new AgentID(receptor));
        msjSalida.setPerformative(performativa);
        msjSalida.setContent(contenido);
        
        if(receptor == NOMBRE_SERVIDOR) {
            msjSalida.setInReplyTo(reply);
            msjSalida.setConversationId(conversationID);
        }        
        System.out.println(getName() + " enviando mensaje a " + receptor + " del tipo " + msjSalida.getPerformative() + " Contenido: " + contenido + "ConvID: " + conversationID + "Reply: " + reply);
        this.send(msjSalida);
    }      
}
