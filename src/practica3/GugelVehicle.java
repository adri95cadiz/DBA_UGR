/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

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
    
    public GugelVehicle(AgentID id) throws Exception{
        super(id);
    }
    
    public void init(){
        System.out.println("Iniciandose " + getName());
        msjEntrada = null;
        msjSalida = null;
        fin = false;        
    }
    
    public void execute(){
        while(!fin){
            try {
                msjEntrada = receiveACLMessage();
                System.out.println(getName() + " ha recibido: " + msjEntrada.getContent());
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
        msjSalida = new ACLMessage();
        msjSalida.setSender(this.getAid());
        msjSalida.setReceiver(new AgentID(receptor));
        msjSalida.setPerformative(performativa);
        msjSalida.setContent(contenido);
        System.out.println(getName() + " enviando mensaje a " + receptor + " del tipo " + msjSalida.getPerformative() + " y el contenido " + contenido);
        this.send(msjSalida);
    }
    
    
}
