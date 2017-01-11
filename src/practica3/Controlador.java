package practica3;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.awt.Point;
import java.util.HashMap;
import javafx.util.Pair;


import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import edu.emory.mathcs.backport.java.util.Arrays;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    //private Celda[][] mapa = new Celda[TAM_MAPA][TAM_MAPA];
    //private double[][] scanner = new double[TAM_MAPA][TAM_MAPA];   
    private boolean objetivoEncontrado;
    private Point puntoObjetivo = new Point();
    
    private boolean cont2;
    int cont;  
   
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
        flota.put("Vehiculo0", null);
        flota.put("Vehiculo1", null);
        flota.put("Vehiculo2", null);
        flota.put("Vehiculo3", null);  
        conversationID = null;
        objetivoEncontrado = false;
        puntoObjetivo = null;
                
        cont = 0;
        cont2 = false;
    }
    
    public void execute() {
        while(!fin) {            
            /*
            En faseRepostar() hay un par de condiciones para que la prueba
            inicial no se alargue hasta el infinito. Las he puesto ahí ya que
            es el último paso del ciclo de paso de mensajes y así no
            perdemos mensajes en el cambio de estados.
            Si al ejecutar veis errores posiblemente sean porque el 
            coche se ha estrellado.
            */            
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
                            faseRepostar();
                            break;
                        case PERCIBIR:
                            fasePercibir();                            
                            break;
                        case OBJETIVO_ENCONTRADO:
                            faseObjetivoEncontrado();
                            break;                            
                    }
                    break;
                case OBJETIVO_ENCONTRADO:
                    switch (subEstadoEncontrado) {
                        case ELECCION_VEHICULO:
                            faseEleccionVehiculo();
                            if(vehiculoElegido.equals("")) {
                                estadoActual = Estado.FINALIZAR;
                            } else {
                                System.out.println("El vehiculo elegido es: " + vehiculoElegido);
                                subEstadoEncontrado = Estado.MOVER;
                            }
                            break;
                        case MOVER:
                            faseMover();
                            break;
                        case REPOSTAR:
                            faseRepostar();
                            break;
                        case PERCIBIR:
                            fasePercibir();
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
        System.out.println(getName() + " enviando mensaje a " + receptor + " del tipo " + msjSalida.getPerformative() + " contenido " + contenido);
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
                JSON.leerKey(mensaje.getContent()); 
                conversationID = mensaje.getConversationId();
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
    private void inicializarPropiedadesVehiculo() {
        String contenido, nomVehiculo;
        ACLMessage mensaje = null;
        PropiedadesVehicle propiedades;
        Percepcion percepcion;
        
        for(String vehiculo : flota.keySet()) {
            contenido = "";
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
        inicializarPropiedadesVehiculo();
        if(estadoActual != Estado.FINALIZAR) {
            estadoActual = Estado.BUSCAR;
        }
        if(tamMapa == 0) {
            estadoActual = Estado.INICIAL;
        }
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
        ACLMessage mensaje = null;
        try {
            mensaje = receiveACLMessage();
            if(mensaje.getPerformativeInt() == ACLMessage.AGREE) {
                JSON.leerKey(mensaje.getContent());
            }            
            // Segundo mensaje para recibir la traza. (ADAPATARLA)
            mensaje = receiveACLMessage(); 
            if(mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                System.out.println("TRAZA: " + mensaje.getContent());
                try {
                    resultadoTraza( mensaje );
                } catch (IOException ex) {
                    Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
                }
            } 
            System.out.println("Se finaliza la sesión de trabajo.");
        } catch (InterruptedException ex) {
	    System.err.println(ex.toString());
        } 
    }
    
    /**
     * Elegimos el vehículo mas adecuado para la ocasión.
     */
    private void faseEleccionVehiculo() {
        System.out.println("\n\tFASE ELECCION VEHICULO.");
        if(buscando) {
            /*
            Caso en el cual aún no sabemos donde esta el punto objetivo,
            es decir, estarían todos en modo "explorador".
            ¿Utilizar solo los de menor consumo y mayor campo de vision?
            */
            vehiculoElegido = "Vehiculo0";
        } else {
            /*
            Ya no estamos buscando por lo que conocemos el punto.            
            */
            vehiculoElegido = "Vehiculo1";
        }
        
    }
    
    /**
     * Elegimos el movimiento mas optimo para el vehiculo seleccionado.
     */
    private void faseMover() {
        System.out.println("FASE MOVER.");
        if(buscando) {
            // Para el caso en el que aún no sabemos donde esta el punto objetivo.
            mover();
        } else {
            // Ya sabemos el objetivo.
            mover();
        }
        
        /* Esta declarado en mover() un string con la decision del
        movimiento, podría ser declarado aqui ya que es donde 
        se decide y pasarse en la función.
        */
    }
    
    /**
     * Ejecutamos el movimiento elegido para el vehiculo elegido.
     * @author Luis Gallego Quero
     */
    private void mover() {
        System.out.println("MOVER().");
        String decision = "moveE";        
        cont++; 
        if(decision.contains("logout")) {
            System.out.println("No se donde moverme.");
            subEstadoBuscando = Estado.ELECCION_VEHICULO;
            subEstadoEncontrado = Estado.ELECCION_VEHICULO;
        } else {
            enviarMensaje(vehiculoElegido, ACLMessage.REQUEST, JSON.mover(decision));
            subEstadoBuscando = Estado.PERCIBIR;
            subEstadoEncontrado = Estado.PERCIBIR;
            try {            
                ACLMessage mensaje = receiveACLMessage();
                if(mensaje.getPerformativeInt() != ACLMessage.INFORM) {
                    // Si al moverse no nos llega un INFORM posiblemente se haya estrellado. 
                    System.out.println(mensaje.getPerformative() + ": " + mensaje.getContent());
                    subEstadoBuscando = Estado.ELECCION_VEHICULO;
                    subEstadoEncontrado = Estado.ELECCION_VEHICULO;
                } else {
                    System.out.println("Mensaje recibido en mover(): " + mensaje.getContent());
                }
            } catch (InterruptedException ex) {
                System.err.println(ex.toString());
                estadoActual = Estado.FINALIZAR;
            }
        }
    }
    
    /**
     * Recibimos y procesamos la percepción que va obteniendo cada vehiculo.
     * @author Luis Gallego Quero
     */
    private void fasePercibir() {
        System.out.println("FASE PERCIBIR.");
        enviarMensaje(vehiculoElegido, ACLMessage.QUERY_REF, "");
        try{
            ACLMessage mensaje = receiveACLMessage();
            System.out.println(mensaje.getPerformative() + ": " + mensaje.getContent());
            if(mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                String nombreVehiculo = mensaje.getSender().name;
                PropiedadesVehicle propiedades = flota.get(nombreVehiculo);
                Percepcion percepcion = JSON.getPercepción(mensaje.getContent());
                percepcion.setNombreVehicle(nombreVehiculo);
                propiedades.actualizarPercepcion(percepcion);
                flota.put(nombreVehiculo, propiedades); 
                /* Entiendo que el "goal" del "result" te indica si está en 
                el objetivo o nó, si es asi, en ese caso el x e y se corresponde
                con el punto objetivo. Supongo que será así.
                */
                if(estadoActual == Estado.BUSCAR && percepcion.getLlegado()) {
                    objetivoEncontrado = percepcion.getLlegado();
                    puntoObjetivo = percepcion.getGps();
                    //objetivoEncontrado();
                    subEstadoBuscando = Estado.OBJETIVO_ENCONTRADO;
                }
                System.out.println("FIN FASE PERCIBIR.");
            }
        } catch (InterruptedException ex) {
	    System.err.println(ex.toString());
	    estadoActual = Estado.FINALIZAR;
	}
        subEstadoBuscando = Estado.REPOSTAR;
	subEstadoEncontrado = Estado.REPOSTAR;
    }
    
    /**
     * Fase de repostaje.
     * @author Luis Gallego Quero
     */
    private void faseRepostar() {
        /*
        Inicialmente la he dejado que repueste siempre, es decir, con 
        el paso de mensaje de repostar. Habría que hacerle una pequeña
        heuristica de cuando si y cuando no repostar.
        */
        System.out.println("FASE REPOSTAR.");
        subEstadoBuscando = Estado.MOVER;
        subEstadoEncontrado = Estado.MOVER;
        PropiedadesVehicle propiedades = flota.get(vehiculoElegido);
       if(!propiedades.getLlegado()) { // Repostamos si no estamos en objetivo.
            /*
            Añadir condiciones? En plan, no repostar si esta demasiado
            lejos como para conseguir llegar con la energia restante, nose..
            */
           // if(propiedades.getBateria() <= propiedades.getRol().getConsumo()) {
                enviarMensaje(vehiculoElegido, ACLMessage.REQUEST, JSON.repostar());
                try {
                    ACLMessage mensaje = receiveACLMessage();
                    if(mensaje.getPerformativeInt() != ACLMessage.INFORM) {
                        System.out.println(mensaje.getPerformativeInt() + ": " + mensaje.getContent());
                        subEstadoBuscando = Estado.ELECCION_VEHICULO;
                        subEstadoEncontrado = Estado.ELECCION_VEHICULO;
                    }
                } catch (InterruptedException ex) {
                    System.err.println(ex.toString());
                    estadoActual = Estado.FINALIZAR;
                }                
            /*} else {
                subEstadoBuscando = Estado.MOVER;
                subEstadoEncontrado = Estado.MOVER;
            } */
        } 
        System.out.println("FIN FASE REPOSTAR.");
        
        /*if(cont == 3 && !cont2) {
                System.out.println("Entra cont == 3.");
                cont2 = true;
                estadoActual = Estado.BUSCAR;
                subEstadoBuscando = Estado.OBJETIVO_ENCONTRADO;
        }*/
        
        if(cont == 5) {
                System.out.println("Entra cont == 5.");
                estadoActual = Estado.FINALIZAR;
        }
    } 
    
    /**
     * Fase objetivo encontrado.
     * @author Luis Gallego Quero
     */
    private void faseObjetivoEncontrado() {
        System.out.println("FASE OBJETIVO ENCONTRADO.");
        if(buscando) {
            buscando = false;
            estadoActual = Estado.OBJETIVO_ENCONTRADO;
            subEstadoEncontrado = Estado.ELECCION_VEHICULO;
        }
    }
    
    /*private void objetivoEncontrado() {
        subEstadoBuscando = Estado.OBJETIVO_ENCONTRADO;
    }*/
    
    private void resultadoTraza( ACLMessage msjEntrada ) throws FileNotFoundException, IOException {
        System.out.println("Recibiendo respuesta traza.");
       
            

            JsonObject injson = Json.parse(msjEntrada.getContent()).asObject();
            
            System.out.println("Esto es lo que contiene la traza en json: " + injson);
            
            JsonArray ja = injson.get("trace").asArray();
            byte data[] = new byte[ja.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) ja.get(i).asInt();
            }
            FileOutputStream fos = new FileOutputStream("traza_" + this.mundo + ".png");
            fos.write(data);
            fos.close();
            System.out.println("Traza Guardada");
       
    }
}
