package practica3;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.awt.Point;
import java.util.HashMap;

/**
 *
 * @author Luis Gallego Quero
 */
public class Controlador extends SingleAgent {
    
    private final String NOMBRE_CONTROLADOR = "Haldus";
    private final short TAM_MAPA = 500;
    private short tamMapa = 0;
    private String mundo;
    private String vehiculoElegido;
    private Estado estadoActual, subEstadoBuscando, subEstadoEncontrado;
    private boolean fin, buscando;
    private HashMap<String, PropiedadesVehicle> flota;
    private Celda[][] mapa = new Celda[TAM_MAPA][TAM_MAPA];
    private double[][] scanner = new double[TAM_MAPA][TAM_MAPA];
    
    
    
    public Controlador(AgentID id, String mundo) throws Exception {
        super(id);
        this.mundo = mundo;
    }
    
    public void init() {
        System.out.println("Iniciandose " + getName());
        flota = new HashMap<>();
        flota.put("Vehiculo10", null);
        flota.put("Vehiculo11", null);
        flota.put("Vehiculo12", null);
        flota.put("Vehiculo13", null);        
        fin = false;
        buscando = true;
        estadoActual = Estado.INICIAL;
        subEstadoBuscando = Estado.ELECCION_VEHICULO;
        subEstadoEncontrado = Estado.ELECCION_VEHICULO;
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
                            break;
                        case MOVER:
                            break;
                        case REPOSTAR:
                            break;
                        case PERCIBIR:
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
        System.out.println(getName() + " enviando mensaje a " + receptor + " del tipo " + msjSalida.getPerformative() + " y contenido " + contenido);
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
                JSON.guardarKey(mensaje.getContent());
                for(String vehiculo : flota.keySet()) {
                    contenido = JSON.registrarse();
                    enviarMensaje(vehiculo, ACLMessage.REQUEST, contenido);
                }
                for(int i=0; i<flota.size(); i++) {
                    mensaje = receiveACLMessage();
                    if(mensaje != null && mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                        PropiedadesVehicle propiedades = new PropiedadesVehicle();
                        propiedades.setRol(JSON.getRol(mensaje.getContent()));
                        flota.put(mensaje.getSender().name, propiedades);
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
    private void inicializarPropiedadesDrone() {
        String contenido, nomVehiculo;
        ACLMessage mensaje = null;
        PropiedadesVehicle propiedades;
        Percepcion percepcion;
        
        for(String vehiculo : flota.keySet()) {
            contenido = JSON.key();
            enviarMensaje(vehiculo, ACLMessage.QUERY_REF, contenido);
        }
        
        try {
            for(int i=0; i<flota.size(); i++) {
                mensaje = receiveACLMessage();
                if(mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                    nomVehiculo = mensaje.getSender().name;
                    propiedades = flota.get(nomVehiculo);
                    percepcion = JSON.getPercepción(mensaje.getContent());
                    percepcion.setNombreVehicle(nomVehiculo);
                    propiedades.actualizarPercepcion(percepcion);
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
        inicializarPropiedadesDrone();
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
        String llave = JSON.key();
        for(String vehiculo : flota.keySet()) {
            enviarMensaje(vehiculo, ACLMessage.CANCEL, llave);
        }
        enviarMensaje(NOMBRE_CONTROLADOR, ACLMessage.CANCEL, llave);
    }
    
}
