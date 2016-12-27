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
    
    private final String NOMBRE_CONTROLADOR = "Controlador2_";
    private final String NOMBRE_SERVIDOR = "Haldus";
    private ACLMessage msjEntrada, msjSalida;
    String receptor;
    boolean fin;
    private String conversationID;
    boolean primero;
    private HashMap<String, String> reply;
    
    public GugelVehicle(AgentID id) throws Exception{
        super(id);
    }
    
    public void init(){
        System.out.println("Iniciandose " + getName());
        msjEntrada = null;
        msjSalida = null;
        fin = false;
        conversationID = null;
        primero = true;        
        reply = new HashMap<>();
        reply.put("Vehiculo10", null);
        reply.put("Vehiculo11", null);
        reply.put("Vehiculo12", null);
        reply.put("Vehiculo13", null); 
    }   
    
    public void execute(){
        while(!fin){
            try {
                msjEntrada = receiveACLMessage();
                System.out.println(getName() + " ha recibido: " + msjEntrada.getContent() + " " + msjEntrada.getConversationId() + " " + msjEntrada.getReplyWith());
                conversationID = msjEntrada.getConversationId();
                if(!msjEntrada.getReplyWith().isEmpty()) {
                    reply.put(getName(), msjEntrada.getReplyWith());   
                }                             
                if(msjEntrada.getPerformativeInt() == ACLMessage.CANCEL){
                    fin = true;                    
                } else {
                    if(msjEntrada.getSender().name.equals(NOMBRE_SERVIDOR)){
                        receptor = NOMBRE_CONTROLADOR;
                    } else {
                        receptor = NOMBRE_SERVIDOR;
                    }
                    //System.out.println("El reply de " + getName() + " vale " + reply.get(getName()));
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
        msjSalida = new ACLMessage();
        msjSalida.setSender(this.getAid());
        msjSalida.setReceiver(new AgentID(receptor));
        msjSalida.setPerformative(performativa);
        msjSalida.setContent(contenido);
        msjSalida.setConversationId(conversationID);
        msjSalida.setInReplyTo(reply.get(getName()));
        System.out.println(getName() + " enviando mensaje a " + receptor + " del tipo " + msjSalida.getPerformative() + " contenido " + contenido + " " + conversationID + " " + reply.get(getName()));
        this.send(msjSalida);
    }      
}
