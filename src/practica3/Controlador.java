package practica3;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.awt.Point;
import java.util.HashMap;
import javafx.util.Pair;

/**
 *
 * @author Luis Gallego Quero
 */
public class Controlador extends SingleAgent {
    
    private final String NOMBRE_CONTROLADOR = "Haldus";
    private final short TAM_MAPA = 500;
    private short tamMapa = 0;
    private String mundo;
    private String conversationID; 
    private String vehiculoElegido;
    private Estado estadoActual, subEstadoBuscando, subEstadoEncontrado;
    private boolean fin, buscando;
    private HashMap<String, PropiedadesVehicle> flota;
    private HashMap<String, String> reply;
    private Celda[][] mapa = new Celda[TAM_MAPA][TAM_MAPA];
    private double[][] scanner = new double[TAM_MAPA][TAM_MAPA];   
    
    //private Point puntoObjetivo = new Point();
   
    public Controlador(AgentID id, String mundo) throws Exception {
        super(id);
        this.mundo = mundo;
    }
    
    public void init() {
        System.out.println("Iniciandose " + getName());
        fin = false;
        buscando = true;
        estadoActual = Estado.INICIAL;
        subEstadoBuscando = Estado.ELECCION_VEHICULO;
        subEstadoEncontrado = Estado.ELECCION_VEHICULO;
        flota = new HashMap<>();
        flota.put("Vehiculo10", null);
        flota.put("Vehiculo11", null);
        flota.put("Vehiculo12", null);
        flota.put("Vehiculo13", null);  
        conversationID = null;
        reply = new HashMap<>();
        reply.put("Vehiculo10", null);
        reply.put("Vehiculo11", null);
        reply.put("Vehiculo12", null);
        reply.put("Vehiculo13", null); 
    }
    
    public void execute() {
        while(!fin) {
            System.out.println("Execute fin: " + fin + " fase: " + estadoActual);
            switch (estadoActual) {
                case INICIAL:
                    faseInicial();
                    break;
                case BUSCAR:
                    switch (subEstadoBuscando) {
                        case ELECCION_VEHICULO:
                            faseEleccionVehiculo();
                            if(vehiculoElegido.equals("")) {
                                estadoActual = Estado.FINALIZAR;
                            } else {
                                System.out.println("El vehiculo elegido es: " + vehiculoElegido);
                                subEstadoBuscando = Estado.MOVER;
                            }
                            break;
                        case MOVER:
                            faseMover();
                            break;
                        case REPOSTAR:
                            break;
                        case PERCIBIR:
                            fasePercibir();
                            //objetivoEncontrado();
                            break;
                        case OBJETIVO_ENCONTRADO:
                            break;                            
                    }
                    break;
                case OBJETIVO_ENCONTRADO:
                    switch (subEstadoEncontrado) {
                        case ELECCION_VEHICULO:
                            break;
                        case MOVER:
                            break;
                        case REPOSTAR:
                            break;
                        case PERCIBIR:
                            break;
                    }
                    break;
                case FINALIZAR:
                    faseFinalizar();
                    break;
            }            
        }        
    }
    
    @Override
    public void finalize() {
        finalizarConversacion();
        super.finalize();
    }
    
    /**
     * Envia cualquier tipo de mensaje.
     * @param receptor Agente.
     * @param performativa Performativa.
     * @param contenido Cadena a enviar.
     * @author Luis Gallego Quero.
     */
    private void enviarMensaje(String receptor, int performativa, String contenido) {
        ACLMessage msjSalida = new ACLMessage();
        msjSalida.setSender(this.getAid());
        msjSalida.setReceiver(new AgentID(receptor));
        msjSalida.setPerformative(performativa);
        msjSalida.setContent(contenido);
        msjSalida.setConversationId(conversationID);
        msjSalida.setInReplyTo(reply.get(receptor));
        System.out.println(getName() + " enviando mensaje a " + receptor + " del tipo " + msjSalida.getPerformative() + " contenido " + contenido + " conversationID: " + conversationID + " replyWith " + reply.get(receptor) );
        this.send(msjSalida);
    }
    
    /**
     * Inicializamos la conversacion con el servidor y con los vehiculos.
     * @author Luis Gallego Quero.
     */
    private void iniciarConversacion() {
        String contenido = JSON.suscribirse(mundo);
        enviarMensaje(NOMBRE_CONTROLADOR, ACLMessage.SUBSCRIBE, contenido);
        ACLMessage mensaje = null;
        try {
            mensaje = receiveACLMessage();
            if(mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                JSON.leerKey(mensaje.getContent()); // String key?
                conversationID = mensaje.getConversationId();
                for(String vehiculo : flota.keySet()) {
                    contenido = JSON.registrarse();
                    enviarMensaje(vehiculo, ACLMessage.REQUEST, contenido);
                }                
                for(int i=0; i<flota.size(); i++) {
                    mensaje = receiveACLMessage();
                    if(mensaje != null && mensaje.getPerformativeInt() == ACLMessage.INFORM) {  
                        //System.out.println("Iniciar conversacion bucle inform.");
                        //System.out.println("Reply with vale: " + mensaje.getInReplyTo() + " de " + mensaje.getSender().name);
                        PropiedadesVehicle propiedades = new PropiedadesVehicle();
                        propiedades.setRol(JSON.getRol(mensaje.getContent()));
                        flota.put(mensaje.getSender().name, propiedades);
                        reply.put(mensaje.getSender().name, mensaje.getInReplyTo());
                        //System.out.println("Añadido el reply " + reply.get(mensaje.getSender().name) + " de " + mensaje.getSender().name);
                    }
                }     
            }           
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            estadoActual = Estado.FINALIZAR;
        }
    }
    
    /**
     * Inicializamos las propiedades de cada vehiculo.
     * @author Luis Gallego Quero.
     */
    private void inicializarPropiedadesVehiculo() {
        String contenido, nomVehiculo;
        ACLMessage mensaje = null;
        PropiedadesVehicle propiedades;
        Percepcion percepcion;
        
        for(String vehiculo : flota.keySet()) {
            contenido = "";
            //System.out.println("Inicializando propiedades del vehiculo: " + vehiculo);
            enviarMensaje(vehiculo, ACLMessage.QUERY_REF, contenido);
        }
        
        try {
            for(int i=0; i<flota.size(); i++) {
                mensaje = receiveACLMessage();
                if(mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                    reply.put(mensaje.getSender().name, mensaje.getInReplyTo());
                    //System.out.println("Añadido el reply " + reply.get(mensaje.getSender().name) + " de " + mensaje.getSender().name);
                    nomVehiculo = mensaje.getSender().name;
                    propiedades = flota.get(nomVehiculo);
                    percepcion = JSON.getPercepción(mensaje.getContent());
                    percepcion.setNombreVehicle(nomVehiculo);
                    propiedades.actualizarPercepcion(percepcion);
                    //System.out.println(nomVehiculo + propiedades.getRol().getConsumo());
                    flota.put(nomVehiculo, propiedades);
                    if(percepcion.getGps().y == 99) {
                        tamMapa = 100;
                    } else if(percepcion.getGps().y == 499 || percepcion.getGps().x >= 100) {
                        tamMapa = 500;
                    }
                    System.out.println(mensaje.getContent());
                }
            }            
        } catch (InterruptedException ex) {
	    System.err.println(ex.toString());
	    estadoActual = Estado.FINALIZAR;
	}       
    }
    
    /**
     * Estado inicial.
     * @author Luis Gallego Quero.
     */        
    private void faseInicial() {
        iniciarConversacion();
        inicializarPropiedadesVehiculo();
        estadoActual = Estado.FINALIZAR;
        /*if(estadoActual != Estado.FINALIZAR) {
            estadoActual = Estado.BUSCAR;
        }
        if(tamMapa == 0) {
            estadoActual = Estado.INICIAL;
        }*/
    }
    
    /**
     * Estado finalizar.
     */    
    private void faseFinalizar() {
        fin = true;
    }
    
    /**
     * Finaliza la conversación con los vehiculos y con el servidor.
     * @author Luis Gallego Quero
     */
    private void finalizarConversacion() {
        for(String vehiculo : flota.keySet()) {
            enviarMensaje(vehiculo, ACLMessage.CANCEL, "");
        }
        enviarMensaje(NOMBRE_CONTROLADOR, ACLMessage.CANCEL, "");
    }
    
    private void faseEleccionVehiculo() {
        
        if(buscando) {
            /*
            Caso en el cual aún no sabemos donde esta el punto objetivo,
            es decir, estarían todos en modo "explorador".
            ¿Utilizar solo los de menor consumo y mayor campo de vision?
            */
            vehiculoElegido = "Vehiculo10";
        } else {
            //objetivoEncontrado();
            /* Comprobamos si se ha encontrado el objetivo en el mapa, en caso
            afirmativo tomamos el punto y con el vehiculo mas cercano a el
            no movemos.
            */
        }
        
    }
    
    private void faseMover() {
        if(buscando) {
            // Para el caso en el que aún no sabemos donde esta el punto objetivo.
            mover();
        } else {
            // Ya sabemos el objetivo, movemos el vehiculo mas adecuado.
        }
    }
    
    private void mover() {
        // Movemos a la posicion mas optima.
        String decision = "moveSE";
        vehiculoElegido = "Vehiculo10";
        enviarMensaje(vehiculoElegido, ACLMessage.REQUEST, JSON.mover(decision));
        subEstadoBuscando = Estado.PERCIBIR;
        subEstadoEncontrado = Estado.PERCIBIR;
        try {            
            ACLMessage mensaje = receiveACLMessage();
            if(mensaje.getPerformativeInt() != ACLMessage.INFORM) {
                System.out.println("RECIBIENDO MENSAJE EN mover()");
                reply.put(mensaje.getSender().name, mensaje.getInReplyTo());
                System.out.println(mensaje.getPerformative() + ": " + mensaje.getContent());
                //estadoActual = Estado.FINALIZAR;
                //faseFinalizar();
               subEstadoBuscando = Estado.ELECCION_VEHICULO;
                subEstadoEncontrado = Estado.ELECCION_VEHICULO;
            } else {
                System.out.println("Mensaje recibido: " + mensaje.getContent());
            }
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            estadoActual = Estado.FINALIZAR;
        }
    }
    
    private void fasePercibir() {
        enviarMensaje(vehiculoElegido, ACLMessage.QUERY_REF, "");
        try{
            ACLMessage mensaje = receiveACLMessage();
            System.out.println(mensaje.getPerformative() + ": " + mensaje.getContent());
            if(mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                String nombreVehiculo = mensaje.getSender().name;
                reply.put(nombreVehiculo, mensaje.getInReplyTo());
                PropiedadesVehicle propiedades = flota.get(nombreVehiculo);
                Percepcion percepcion = JSON.getPercepción(mensaje.getContent());
                percepcion.setNombreVehicle(nombreVehiculo);
                propiedades.actualizarPercepcion(percepcion);
                flota.put(nombreVehiculo, propiedades); 
            }
        } catch (InterruptedException ex) {
	    System.err.println(ex.toString());
	    estadoActual = Estado.FINALIZAR;
	}
        //subEstadoBuscando = Estado.FINALIZAR;
	//subEstadoEncontrado = Estado.FINALIZAR;
        subEstadoBuscando = Estado.REPOSTAR;
	subEstadoEncontrado = Estado.REPOSTAR;
    }
    
}
