package practica3;

import java.util.HashMap;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import static java.lang.Math.floor;
import java.util.logging.Level;
import java.util.logging.Logger;
import Interface.Ventana;
import jason.stdlib.intend;

/**
 *
 * @author Luis Gallego Quero
 */
public class Controlador extends SingleAgent {

    private final String NOMBRE_SERVIDOR = "Haldus";
    // private final short TAM_MAPA = 500;
    private String mundo;
    private String conversationID;
    private String vehiculoElegido;
    private boolean vehiculoSeleccionado = false;
    private boolean vehiculoSelEsp = false;
    private Estado estadoActual, subEstadoBuscando, subEstadoEncontrado;
    private boolean fin, buscando;
    private HashMap<String, PropiedadesVehicle> flota;
    //private Celda[][] mapa = new Celda[TAM_MAPA][TAM_MAPA];
    //private double[][] scanner = new double[TAM_MAPA][TAM_MAPA];   
    private boolean objetivoEncontrado;
    private boolean triedPath = false;
    //private Cell puntoObjetivo = new Cell();
    ArrayList<int[]> objetivos = new ArrayList<>();
    ArrayList<String> vehiculosExploradores = new ArrayList<>();
    ArrayList<String> vehiculosEsperando = new ArrayList<>();
    ArrayList<String> vehiculosFinalizados = new ArrayList<>();
    ArrayList<int[]> posAgentsEnd = new ArrayList<>();
    boolean cambio_de_vehiculo = false;
    private int[][] posiblesObjetivos;
    private boolean cont2;
    boolean exist_path = false;
    Path camino;
    int cont;
    private int max_Pos = 0;
    private ArrayList<Integer> path_local = new ArrayList<>();
    // Valores modificables según que comportamiento del agene deseamos    
    private final int MAPA = main.MAPA;
    private boolean check = true;
    private final int LIMITE_PASOS = 1000;
    private final boolean EXPLORAR = false;
    private Ventana miVentana;


    public Controlador(AgentID id, String mundo) throws Exception {
        super(id);
        this.mundo = mundo;
    }

    public void init() {
        //System.out.println("Iniciandose Controlador " + getName());
        fin = false;
        buscando = true;                
        miVentana = new Ventana();
        miVentana.setVisible(true);
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
        miVentana.setMapaConocimiento(Knowledge.getDB(this.MAPA).drawMapToString());
        cont = 0;
        cont2 = false;
    }

    public void execute() {
        while (!fin) {
            /*
            En faseRepostar() hay un par de condiciones para que la prueba
            inicial no se alargue hasta el infinito. Las he puesto ahí ya que
            es el último paso del ciclo de paso de mensajes y así no
            perdemos mensajes en el cambio de estados.
            Si al ejecutar veis errores posiblemente sean porque el 
            coche se ha estrellado.
             */
            //System.out.println("Execute fin: " + fin + "\nFase: " + estadoActual);
            switch (estadoActual) {
                case INICIAL:
                    faseInicial();
                    monitorizarVehiculos();
                    if (Knowledge.getDB(this.MAPA).contains(Knowledge.STATE_GOAL)) {
                        ArrayList<Cell> obj = Knowledge.getDB(this.MAPA).getObjetives();
                        for(int i = 0; i < obj.size() ; i++){
                            objetivos.add(new int[]{obj.get(i).getPosX(),obj.get(i).getPosY()});
                        }
                        estadoActual = Estado.OBJETIVO_ENCONTRADO;
                    }
                    break;
                case BUSCAR:
                    switch (subEstadoBuscando) {
                        case ELECCION_VEHICULO:
                            faseEleccionVehiculo();
                            subEstadoBuscando = Estado.MOVER;
                            break;
                        case MOVER:
                            faseMover();
                            subEstadoBuscando = Estado.PERCIBIR;
                            break;
                        case REPOSTAR:
                            faseRepostar();
                            // no se cambia de estado porque se llama desde faseMover
                            break;
                        case PERCIBIR:
                            fasePercibir();
                            subEstadoBuscando = Estado.ELECCION_VEHICULO;
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
                            if (vehiculoElegido.equals("")) {
                                estadoActual = Estado.FINALIZAR;
                            } else {
                                //System.out.println("El vehiculo elegido es: " + vehiculoElegido);
                                subEstadoEncontrado = Estado.MOVER;
                            }
                            break;
                        case MOVER:
                            faseMover();
                            subEstadoEncontrado = Estado.PERCIBIR;
                            break;
                        case REPOSTAR:
                            faseRepostar();
                            break;
                        case PERCIBIR:
                            fasePercibir();
                            subEstadoEncontrado = Estado.ELECCION_VEHICULO;
                            break;
                    }
                    break;
                case FINALIZAR:
                    //faseFinalizar();
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
     *
     * @param receptor Agente.
     * @param performativa Performativa.
     * @param contenido Cadena a enviar.
     * @author Luis Gallego Quero.
     */
    private void enviarMensaje(String receptor, int performativa, String contenido) {
        //System.out.println("\nEn el enviar Mensaje del controlador: \n");
        ACLMessage msjSalida = new ACLMessage();
        msjSalida.setSender(this.getAid());
        msjSalida.setReceiver(new AgentID(receptor));
        msjSalida.setPerformative(performativa);
        msjSalida.setContent(contenido);
        msjSalida.setConversationId(conversationID);
        //System.out.println(getName() + " enviando mensaje a " + receptor + " del tipo " + msjSalida.getPerformative() + " contenido " + contenido);
        this.send(msjSalida);
    }

    /**
     * Inicializamos la conversacion con el servidor y con los vehiculos.
     *
     * @author Luis Gallego Quero.
     */
    private void iniciarConversacion() {
        //System.out.println("\n\tINICIO CONVERSACION");
        String contenido = JSON.suscribirse(mundo);
        enviarMensaje(NOMBRE_SERVIDOR, ACLMessage.SUBSCRIBE, contenido);
        ACLMessage mensaje;
        try {
            mensaje = receiveACLMessage();

            if (mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                JSON.leerKey(mensaje.getContent());
                conversationID = mensaje.getConversationId();
                for (String vehiculo : flota.keySet()) {
                    contenido = JSON.registrarse();
                    enviarMensaje(vehiculo, ACLMessage.REQUEST, contenido);
                }
                for (int i = 0; i < flota.size(); i++) {
                    mensaje = receiveACLMessage();

                    if (mensaje != null && mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                        PropiedadesVehicle propiedades = new PropiedadesVehicle();

                        ////System.out.println("Esto es lo que deberían de ser las propiedades: " + mensaje.getContent());
                        propiedades.setRol(JSON.getRol(mensaje.getContent()));
                        flota.put(mensaje.getSender().name, propiedades);
                    }
                }
            }
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            estadoActual = Estado.FINALIZAR;
        }
        //System.out.println("\tFIN INICIO CONVERSACION");
    }

    /**
     * Inicializamos las propiedades de cada vehiculo.
     *
     * @author Luis Gallego Quero.
     */
    private void inicializarPropiedadesVehiculo() { // En realidad las propiedades del vehículo se podrían coger antes, en la inicialización
        //System.out.println("\n\tINICIO PROPIEDADES VEHICULO");
        String contenido, nomVehiculo;
        ACLMessage mensaje = null;
        PropiedadesVehicle propiedades;
        Percepcion percepcion;

        for (String vehiculo : flota.keySet()) {
            contenido = "";
            enviarMensaje(vehiculo, ACLMessage.QUERY_REF, contenido); //Petición de percepción
        }
        try {
            for (int i = 0; i < flota.size(); i++) {
                mensaje = receiveACLMessage();
                if (mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                    nomVehiculo = mensaje.getSender().name;
                    propiedades = flota.get(nomVehiculo);

                    percepcion = JSON.getPercepcion(mensaje.getContent());
                    percepcion.setNombreVehicle(nomVehiculo);
                    propiedades.setMatrix(new GugelVehicleMatrix(Knowledge.getDB(this.MAPA), nomVehiculo, propiedades.getRol().getAlcance()));
                  
                    propiedades.actualizarPercepcion(percepcion);
                    propiedades.setPosInicial(JSON.getPosInicial(mensaje.getContent()));

                    flota.put(nomVehiculo, propiedades);

                    System.out.println("\nNombre vehiculo" + propiedades.getNombre());
                    System.out.println("\nRol: " + propiedades.getRol());
                    System.out.println("\nRadar: ");
                    propiedades.printRadar();
                    System.out.println("\nGPS: ");
                    propiedades.printGps();

                    if (propiedades.getGps()[0] > max_Pos) {
                        max_Pos = propiedades.getGps()[0];
                    }
                    if (propiedades.getGps()[1] > max_Pos) {
                        max_Pos = propiedades.getGps()[1];
                    }
                    
                    Knowledge.getDB(this.MAPA).setAgentPosition(propiedades.getNombre(), propiedades.getGps()[1], propiedades.getGps()[0]);

                    //System.out.println(mensaje.getContent());

                }
            }
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            estadoActual = Estado.FINALIZAR;
        }
        //System.out.println("\tFIN PROPIEDADES VEHICULO");
    }

    /**
     * Estado inicial.
     *
     * @author Luis Gallego Quero.
     */
    private void faseInicial() {
        iniciarConversacion();
        inicializarPropiedadesVehiculo();
        if (estadoActual != Estado.FINALIZAR) {
            estadoActual = Estado.BUSCAR;
        }
        for (String vehiculo : flota.keySet()) {
            vehiculosExploradores.add(vehiculo);
        }
        // Cogemos los vehículos que sean coche, que serán los que exploren.
        // Los demás están esperando a que los exploradores lleguen al objetivo.
        /*for (String vehiculo : flota.keySet()) {
            if (flota.get(vehiculo).getRol().getId() == 1 && vehiculosExploradores.size() == 0) {
                vehiculosExploradores.add(vehiculo);
            } else {
                vehiculosEsperando.add(vehiculo);
            }
        } // Si no hay ningún coche cogeremos los camiones
        // Los demás están esperando a que los exploradores lleguen al objetivo.
        if (vehiculosExploradores.isEmpty()) {
            for (String vehiculo : flota.keySet()) {
                if (flota.get(vehiculo).getRol().getId() == 2 && vehiculosExploradores.size() == 0) {
                    vehiculosExploradores.add(vehiculo);
                    vehiculosEsperando.remove(vehiculo);
                }
            }
        } // Si todo son aviones pues los metemos todos en explorar
        // Los demás están esperando a que los exploradores lleguen al objetivo.
        if (vehiculosExploradores.isEmpty()) {
            for (String vehiculo : flota.keySet()) {
                vehiculosExploradores.add(vehiculo);
                vehiculosEsperando.remove(vehiculo);
            }
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
     *
     * @author Luis Gallego Quero
     */
    private void finalizarConversacion() {
        for (String vehiculo : flota.keySet()) {
            enviarMensaje(vehiculo, ACLMessage.CANCEL, "");
        }
        enviarMensaje(NOMBRE_SERVIDOR, ACLMessage.CANCEL, "");
        ACLMessage mensaje;
        try {
            mensaje = receiveACLMessage();
            if (mensaje.getPerformativeInt() == ACLMessage.AGREE) {
                JSON.leerKey(mensaje.getContent());
            }
            //Segundo mensaje para recibir la traza. (ADAPATARLA)
            mensaje = receiveACLMessage();
            if (mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                System.out.println("TRAZA: " + mensaje.getContent());
                try {
                    resultadoTraza(mensaje);
                } catch (IOException ex) {
                    Logger.getLogger(Controlador.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //System.out.println("Se finaliza la sesión de trabajo.");
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
        }
    }

    /**
     * Elegimos el vehículo mas adecuado para la ocasión.
     */
    private void faseEleccionVehiculo() {
        System.out.println("\n\tFASE ELECCION VEHICULO.");
        

            /*
            Caso en el cual aún no sabemos donde esta el punto objetivo,
            es decir, estarían todos en modo "explorador".
            ¿Utilizar solo los de menor consumo y mayor campo de vision?
             */
            System.out.println("size: " + vehiculosExploradores.size());
            System.out.println("seleccionado: " + vehiculoSeleccionado);
            System.out.println("index + 1: " + vehiculosExploradores.indexOf(vehiculoElegido) + 1);

            // Si ya existe un vehiculo seleccionado:
            if (vehiculoSeleccionado) {
                int[][] radar = flota.get(vehiculoElegido).getRadar();
                System.out.println("Es un: " + flota.get(vehiculoElegido).getRol());
                /*System.out.println("raiz: " + (int) Math.sqrt(radar.length));
                System.out.println("-1/2 es: " + ((int) Math.sqrt(radar.length) - 1) / 2);*/
                int posCentral = (radar.length - 1) / 2;
                System.out.println("posicion central es " + posCentral);
                if (radar[posCentral][posCentral] == 3) {
                    System.out.println("Ha llegado al objetivo. Size de exploradores total: " + vehiculosExploradores.size());
                    if (vehiculosExploradores.size() > 1) {
                        //flota.get(vehiculoElegido).updateMatrix();
                        int[] pos = new int[2];
                        pos[0] = flota.get(vehiculoElegido).getGps()[0];
                        pos[1] = flota.get(vehiculoElegido).getGps()[1];
                        posAgentsEnd.add(pos);
                        vehiculosExploradores.remove(vehiculoElegido);
                        vehiculoElegido = vehiculosExploradores.get(0);
                        exist_path = false;
                        triedPath = false;
                    } else if (vehiculosExploradores.size() == 1) {
                        System.out.println("se ha llamado a finalizar");
                        faseFinalizar();
                    }
                }
                /*if (radar[posCentral][posCentral] == 3) {
                    System.out.println("\nHa llegado a una parte del objetivo");
                    // Si el vehiculo actual se encuentra en el objetivo:
                    if (vehiculoSelEsp) {
                        System.out.println("Hay un vehiculo seleccionado");
                        if (vehiculosEsperando.size() > 1) {
                            System.out.println("Coge el primer de esperando");
                            int siguiente = (vehiculosEsperando.indexOf(vehiculoElegido) + 1) % vehiculosEsperando.size();
                            vehiculosFinalizados.add(vehiculoElegido);
                            vehiculosEsperando.remove(vehiculoElegido);
                            vehiculoElegido = vehiculosExploradores.get(siguiente);
                            
                        } else if (vehiculosEsperando.size() == 1) {
                            
                            vehiculosFinalizados.add(vehiculoElegido);
                            vehiculosEsperando.remove(vehiculoElegido);
                            vehiculoElegido = vehiculosEsperando.get(0);
                            vehiculosEsperando.remove(vehiculoElegido);
                        } else {
                            System.out.println("\n\n\n\nWINNER!!!!!");
                        }
                    } else {
                        vehiculosExploradores.remove(vehiculoElegido);
                        vehiculosFinalizados.add(vehiculoElegido);
                        vehiculoElegido = vehiculosEsperando.get(0);
                        vehiculoSelEsp = true;
                    }
                }*/

            } else {
                //Aquí elegiríamos el vehículo siguiente.                 
                vehiculoElegido = vehiculosExploradores.get(0);
                //Elegiremos el que tenga mayor rango de vision
                for(int i = 1 ; i < vehiculosExploradores.size() ; i++){
                    PropiedadesVehicle p1 = flota.get(vehiculosExploradores.get(i));                    
                    PropiedadesVehicle p2 = flota.get(vehiculoElegido);
                    if(p1.getRol().getConsumo() < p2.getRol().getConsumo())
                        vehiculoElegido = vehiculosExploradores.get(i);
                }                
                exist_path = false;
                vehiculoSeleccionado = true;
                triedPath = false;
            }
            //vehiculoElegido = vehiculosExploradores.get(0);

            /**
             * *************** INFORMACION ***********************************
             */
            /*PropiedadesVehicle p = flota.get(vehiculoElegido);

            Rol r = p.getRol();
            System.out.println("\nPropiedades del vehiculo elegido: ");
            System.out.println("\nBateria: " + p.getBateria());
            int[] coord = new int[2];
            coord = p.getGps();
            //System.out.println("\nRadar: "+ java.util.Arrays.deepToString(p.getRadar()));
            //System.out.println("\ncoordenadas: " + coord[0] + "." + coord[1]);
            //System.out.println("\nRol: " + p.getRol());
            //System.out.println("\nAlcance: " + r.getAlcance());
            //System.out.println("\nConsumno: " + r.getConsumo());
            /**
             * ***************************************************************
             */

        System.out.println("termina eleccion vehiculo");

    }

    /**
     * Elegimos el movimiento mas optimo para el vehiculo seleccionado.
     */
     /** 
     * private void faseMover() {
     *  //System.out.println("\n\tFASE MOVER.");
     *  // Utilizar diferentes funciones Mover para el caso buscar.
        if (buscando) {
             Para el caso en el que aún no sabemos donde esta el punto objetivo.
            mover();
        } else {
             Ya sabemos el objetivo.
            mover();
        }

        /* Esta declarado en mover() un string con la decision del
        movimiento, podría ser declarado aqui ya que es donde 
        se decide y pasarse en la función.
         */
    //}

    /**
     * Ejecutamos el movimiento elegido para el vehiculo elegido.
     *
     * @author Luis Gallego Quero
     */
    private void faseMover() {
        System.out.println("entra en mover");
        /*
        Aquí se obtienen y muestran las propiedades del vehículo
         */
        //System.out.println("\n======================================================================");
        PropiedadesVehicle p = flota.get(vehiculoElegido);
        Rol r = p.getRol();
        //System.out.println("Propiedades del vehiculo elegido: ");
        //System.out.println("\nBateria: " + p.getBateria());
        int[] coord = new int[2];
        coord = p.getGps();
        int[][] radar = p.getRadar();
        //System.out.println("\nRadar: "+ java.util.Arrays.deepToString(radar));
        //System.out.println("\ncoordenadas: " + coord[0] + "." + coord[1]);
        //System.out.println("\nRol: " + p.getRol());
        int alcance = r.getAlcance();
        //System.out.println("\nAlcance: " + alcance);
        //System.out.println("\nConsumo: " + r.getConsumo());
        int pasos = p.getPasos();
        //System.out.println("\nPasos dados: " + pasos);
        //System.out.println("======================================================================");
        /*
        Control sobre la batería del vehículo elegido para moverse
         */
        if (flota.get(vehiculoElegido).getBateria() < 10) {
            faseRepostar();
        }
        /*
        1 - Si el objetivo existe y hay camino, de tirón.
        2 - Si no existe un camino óptimo local, se calcula uno aquí.
        3 - Si ya existe un camino óptimo local, se van cogiendo los movimientos
        hasta que llegue a su casilla destino.
        4 - Actualizar Knowledge
        5 - Acción de mover
         */

        //System.out.println("\n\t MOVER()");
        String decision;
        cont++;
        System.out.println("contador: " + cont);

        /*
        Aquí se decide el movimiento
         */
        if(!triedPath && objetivos.size()>0 && !p.getRol().getVolar()){            
            int[] posicion_objetivo = new int[2];
            int[] gps = flota.get(vehiculoElegido).getGps();
            posicion_objetivo = calcularObjetivoCercano(gps); 
            int [][] pathMatrix = Knowledge.getDB(this.MAPA).getPathMatrix();
            /*for(int i=0; i < pathMatrix.length; i++){
                for(int j=0; j< pathMatrix[i].length; j++){
                    System.out.print(pathMatrix[i][j] + " ");
                }
                System.out.println("");
            }*/
            camino = new Path(pathMatrix, p.getGps()[0] * pathMatrix.length + p.getGps()[1], posicion_objetivo[0] * pathMatrix.length + posicion_objetivo[1]);
            path_local.clear();
            path_local = camino.getPath();
            triedPath= true;
            exist_path = true;
        } else if (!exist_path) {                                                 //Si no existe un camino establecido pero tampoco se conoce el espacio entre el vehículo y ningún objetivo,
            System.out.println("Calculando nuevo camino");
            //cambio_de_vehiculo = false;
            System.out.println("Alcance: " + alcance);
            posiblesObjetivos = new int [alcance][alcance];
            eliminarObjetivosInaccesibles(radar.clone(), alcance);
            /*//System.out.println("Posibles Objetivos: " + Arrays.deepToString(posiblesObjetivos));
            for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        System.out.print(posiblesObjetivos[i][j] + "-");
                    }
                    //System.out.println("\n");
                }   */
            // Se decide la casilla óptima a moverse en la matriz 5x5

            int[] objetivo_alcanzar = chooseLocalObj(pasos, coord, p.getNombre(), p.getMatrix());
            System.out.println("imprimiendo objetivo a alcanzar");
            System.out.println("el objativo a alcanzar es el siguiente: " + objetivo_alcanzar[0] + "-" + objetivo_alcanzar[1]);
            System.out.println("sale de chooselocalobj antes de objetivo_id");
            int objetivo_id = objetivo_alcanzar[0] * alcance + objetivo_alcanzar[1];
            System.out.println("calcula el objetivo_id");
            // Se calcula el camino optimo para llegar hasta ella

            /*//System.out.println("RADAR ------------------> ");
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        System.out.print(radar[i][j] + "-");
                    }
                    //System.out.println("\n");
                }
            //System.out.println("");

            //System.out.println("POSIBLES ------------------> ");
            for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        System.out.print(posiblesObjetivos[i][j] + "-");
                    }
                    //System.out.println("\n");
                }
            //System.out.println("");

            //System.out.println("OBJETIVO ID ------------> ");
            //System.out.println(objetivo_id);*/
            // Actualizar el objeto camino con el nuevo radar y la nueva pos final
            /*System.out.println("\n\t\tSENSOR DEL AGENTE");
            for (int i = 0; i < p.getRadar().length; i++) {
                for (int j = 0; j < p.getRadar().length; j++) {
                    System.out.print(p.getRadar()[i][j] + " ");
                }
                System.out.println("");
            }*/
            //eliminarObjetivosInaccesibles(p.getRadar().clone(), alcance);
            /*.out.println("POSIBLES OBJETIVOS");
            for (int i = 0; i < p.getRadar().length; i++) {
                for (int j = 0; j < p.getRadar().length; j++) {
                    System.out.print(posiblesObjetivos[i][j]);
                }
                System.out.println("");
            }*/
            /*System.out.println("\n\t\tSENSOR DEL AGENTE");
            for (int i = 0; i < p.getRadar().length; i++) {
                for (int j = 0; j < p.getRadar().length; j++) {
                    System.out.print(p.getRadar()[i][j] + " ");
                }
                System.out.println("");
            }*/

            //System.out.println("antes de calcular el nuevo camino");
            camino = new Path(posiblesObjetivos, (int) floor((alcance * alcance) / 2.0), objetivo_id);
            //System.out.println("despues de calcular el nuevo camino");
            // camino.changeObjetive(objetivo_id);
            // camino.changeStart((int) floor((alcance * alcance) / 2.0));
            ////System.out.println("CAMBIOS REALIZADOS, OBJETIVO Y MATRIZ");
            ////System.out.println("NUEVO PATH OBTENIDO");
            path_local.clear();
            path_local = camino.getPath();

        }
        cambio_de_vehiculo = false;
        /**
         * Si ya existía un path óptimo: -Calcula la posición a la que se debe
         * de mover según el path -Elimina dicha posición del path -Una vez que
         * se haya completado el path se vuelve a calcular uno
         */
        exist_path = true;
        //System.out.println("CAMINO A SEGUIR:");
        camino.printPath();
        //if(camino.getPath().size() > 1){
        //System.out.println("se mete en camino");
        //System.out.println("primro");
        int primera_casilla = path_local.get(0);
        //System.out.println("segundo");
        int segunda_casilla = path_local.get(1);
        //System.out.println("calcula mov");
        int obj_prox_mov = primera_casilla - segunda_casilla;
        decision = pathLocalObj(obj_prox_mov, radar);
        System.out.println("Mueve a " + decision);
        //}
        // Se transforman las IDs de las casillas a coordenadas para saber
        // identificar la dirección en la que se debe de mover

        ////System.out.println("DIFERENCIA ENTRE CASILLAS --------------> " + obj_prox_mov);
        ////System.out.println("COORDENADAS A MOVERSE POR EL PRIMER PATH -------------------->  " + path_local.get(1) / 5 + "," + path_local.get(1) % 5);
        ////System.out.println("COORDENADAS A MOVERSE POR EL PRIMER PATH: " + obj_prox_mov[0] + obj_prox_mov[1]);*/
        // Se obtiene la dirección en la que moverse
        /**
         * **************
         * Una vez se sabe en que dirección se quiere mover: -Actualiza el mapa
         * de la base de datos -Control sobre número de pasos y batería -Manda
         * la acción del movimiento -Espera a recibir la respuesta
         *
         */
        if (!decision.contains("move")) {
            System.out.println("No se donde moverme.");
            // subEstadoBuscando = Estado.ELECCION_VEHICULO;
            // subEstadoEncontrado = Estado.ELECCION_VEHICULO;
        } else {
            System.out.println("Updateando matrix");
            p.updateMatrix(); 
            System.out.println("Fin update");   
            System.out.println("enviando mensaje a vehiculoElegido");
            enviarMensaje(vehiculoElegido, ACLMessage.REQUEST, JSON.mover(decision));
            System.out.println("mensaje enviado");
            //  subEstadoBuscando = Estado.PERCIBIR;
            //  subEstadoEncontrado = Estado.PERCIBIR;
            try {
                ACLMessage mensaje = receiveACLMessage();
                if (mensaje.getPerformativeInt() != ACLMessage.INFORM) {
                    System.out.println("la performativa no contiene inform");
                    // Si al moverse no nos llega un INFORM posiblemente se haya estrellado. 
                    System.out.println(mensaje.getPerformative() + ": " + mensaje.getContent());
                    //  subEstadoBuscando = Estado.ELECCION_VEHICULO;
                    //  subEstadoEncontrado = Estado.ELECCION_VEHICULO;
                } else {
                    System.out.println("Mensaje recibido en mover(): " + mensaje.getContent());
                }
            } catch (InterruptedException ex) {
                System.err.println(ex.toString());
                estadoActual = Estado.FINALIZAR;
            }
        }
        if (path_local.size() > 0) {
            path_local.remove(0);
            System.out.println("remueve pathlocal");
        }
        // Si el path_local restante contiene solamente una posición, es la 
        // del propio agente por lo que se borra.
        if (path_local.size() == 1) {
            exist_path = false;
            System.out.println("pathlocal == 1");
        }
        //p.getMatrix().ImprimirLocal();        
        //System.out.println("GPS X: " + p.getGps()[0] + " Y: " + p.getGps()[1]);
        ////System.out.println("Datos del GPS bien puestos: " + datosGPS[0] + datosGPS[1] + "\n\t\tPaso numero: " + this.contadorPasos + "\n");
             
        //System.out.println("incrementar paso");
        p.darPaso();
        /*System.out.println("\n\t\tSENSOR DEL AGENTE");
        for(int i=0; i < p.getRadar().length; i++){
            for(int j=0; j< p.getRadar().length; j++){
                System.out.print(p.getRadar()[i][j] + " ");
            }
            System.out.println("");
        }*/
        /*System.out.println("\nimprimir matriz local");
        p.getMatrix().ImprimirLocal();
        //System.out.println("\nImprimir matriz combined");
        //p.getMatrix().ImprimirGetCombined();
        //System.out.println("\nimprimir matriz knowledge");
        System.out.println(p.getMatrix().ImprimirKnow());*/
        System.out.println("\t\tPaso numero: " + p.getPasos());
        
        // devuelve la matriz de la base de datos. 
        /*bd. imprimirMatriz();
        devuelve una matriz, que contiene -1 cuando sea borde del mapa, muro, agente, o inexplorado.
        Y lo que sea explorado y libre o objetivo
        bd.getMatrizAEstrella();*/
        
        /*
        CONTROL PARA DEJAR DE EJECUTAR
        */
        /*if (cont == 100) {
            //System.out.println("Entra cont == 5.");
            //  estadoActual = Estado.FINALIZAR;
            faseFinalizar();
        }*/
        //System.out.println("fin fase mover");
        Knowledge.getDB(this.MAPA).setAgentPosition(p.getNombre(), p.getGps()[1], p.getGps()[0]);
        monitorizarVehiculos();
        miVentana.setMapaConocimiento(Knowledge.getDB(this.MAPA).drawMapToString());
        miVentana.setMapaVehiculo(p.getNombre(), p.getMatrix().drawMapToString());
    }

    /**
     * Pone los objetivos inaccesibles del [alcance x alcance] que rodea al
     * gugelcar a -1 y los accesibles a 0.
     *
     * @author Adrian Portillo Sanchez
     */
    private void eliminarObjetivosInaccesibles(int[][] radar, int alcance) {
        int pos_inicial = (int) floor(alcance / 2.0);
        eliminarObjetivosInaccesiblesRec(radar.clone(), alcance, pos_inicial, pos_inicial);
        posiblesObjetivos[pos_inicial][pos_inicial] = -1;          //Pone la posicion actual a -1, no se deberia deliberar sobre ella.
        for (int i = 0; i < alcance; i++) {                        //Pone los objetivos no alcanzados a -1, tampoco son accesibles.
            for (int j = 0; j < alcance; j++) {
                if (posiblesObjetivos[i][j] == 0) {
                    posiblesObjetivos[i][j] = -1;
                }
            }
        }
        for (int i = 0; i < alcance; i++) {                         //Pone los 1's a 0's dejando finalmente los accesibles a 0 y los inaccesibles a -1.
            for (int j = 0; j < alcance; j++) {
                if (posiblesObjetivos[i][j] == 1) {
                    posiblesObjetivos[i][j] = 0;
                }
            }
        }
    }

    /**
     * Funcion recursiva de eliminarObjetivosInaccesibles
     *
     * @author Adrian Portillo Sanchez
     */
    private void eliminarObjetivosInaccesiblesRec(int[][] radar, int alcance, int row, int col) {
        //System.out.print("eliminar inaccesibles: "+row+col);
        if (row < 0 || row > alcance - 1 || col < 0 || col > alcance - 1) {                         //Se encuentra fuera de los límites
            //System.out.println("fuera de limites "+row+col);
        } else if (posiblesObjetivos[row][col] == -1 || posiblesObjetivos[row][col] == 1) {         //Aunque dentro de los límites ya ha sido recorrida
        
        } else {
            int pos_inicial = (int) floor(alcance / 2.0);
            if ((row != pos_inicial || col != pos_inicial) && ((radar[row][col] == 1 && alcance != 3) || radar[row][col] == 2 || radar[row][col] == 4 || (radar[row][col] == 3 && Knowledge.getDB(MAPA).isAnyAgentInPosition(row, col)))) {
                posiblesObjetivos[row][col] = -1;                                                   //Aunque alcanzable posee un obstáculo en este momento
                //System.out.println("inaccesible "+row+col);
            } else {
                //System.out.println("accesible "+row+col);
                posiblesObjetivos[row][col] = 1;                                                    //Es libre, alcanzable, y dentro de los límites    
                eliminarObjetivosInaccesiblesRec(radar, alcance, row - 1, col - 1);	//Superior izquierdo.
                eliminarObjetivosInaccesiblesRec(radar, alcance, row - 1, col);         //Superior centro.
                eliminarObjetivosInaccesiblesRec(radar, alcance, row - 1, col + 1);	//Superior derecho.
                eliminarObjetivosInaccesiblesRec(radar, alcance, row, col - 1);         //Centro izquierdo.
                eliminarObjetivosInaccesiblesRec(radar, alcance, row, col + 1);         //Centro derecho.
                eliminarObjetivosInaccesiblesRec(radar, alcance, row + 1, col - 1);	//Inferior izquierdo.
                eliminarObjetivosInaccesiblesRec(radar, alcance, row + 1, col);     	//Inferior centro.
                eliminarObjetivosInaccesiblesRec(radar, alcance, row + 1, col + 1);	//Inferior derecho.	
            }
        }
    }
    
    /**
     * *************************************************************************
     * @author Raúl López Arévalo, Adrián Portillo Sánchez
     *
     * Elige el objetivo conocido más cercano a una posición dada y lo devuelve.
     *
     * @return objetivo Devuelve las coordenadas del objetivo más cercano como int[].
     */
    private int[] calcularObjetivoCercano(int[] gps){
        int[] posicion_objetivo = new int[2];
        int[] minimo_objetivo = {100000,100000};
        // Si se han encontrado 2 objetivos:
        for(int i = 0 ; i < objetivos.size() ; i++){ 
            //System.out.println("\nObjetivo: " +objetivos.get(i)[0] +","+objetivos.get(i)[1]);
            // Calcula el gradiente en la posición del agente para cada objetivo               
            int dist1 = Math.abs(objetivos.get(i)[0] - gps[0]) + Math.abs(objetivos.get(i)[1] - gps[1]);
            int dist2 = Math.abs(minimo_objetivo[0]- gps[0]) + Math.abs(minimo_objetivo[1] - gps[1]);                
            // Si el objetivo nuevo es menor que el mínimo actual lo cogemos como destino
            if (dist1 < dist2) 
                minimo_objetivo[0] = objetivos.get(i)[0];
                minimo_objetivo[1] = objetivos.get(i)[1];                
        }           
        posicion_objetivo[0] = minimo_objetivo[0];
        posicion_objetivo[1] = minimo_objetivo[1];
        return posicion_objetivo;
    }
    /**
     * *************************************************************************
     * @author Raúl López Arévalo, Adrián Portillo Sánchez
     *
     * Determina la mejor posición a la que moverse dentro del mapa local
     * [alcance x alcance]. En el primer movimiento decide la casilla objetivo
     * teniendo en cuenta si es accesible y la cercanía al objetivo.
     *
     * A partir del primer movimiento para decidir esta casilla tiene en cuenta
     * el mapa de la base de datos.
     *
     * @return objetivo Devuelve las coordenadas de la casilla como int[].
     */
    private int[] chooseLocalObj(int pasos, int[] datosGPS, String nombre, GugelVehicleMatrix matriz) {

        System.out.println("\nFase elegir objetivo");

        // comienzo */
        int alcance = flota.get(vehiculoElegido).getRol().getAlcance();
        int[][] matrixGrad = new int[alcance][alcance];
        int[] posicion_objetivo = new int[2];
        int[][] global = matriz.getKnowledgeMatrix();
        //System.out.println("Objetivos: "+ objetivos.toString());
        if (estadoActual == Estado.OBJETIVO_ENCONTRADO || objetivos.size() > 0) {
            int[] gps = flota.get(vehiculoElegido).getGps();
            posicion_objetivo = calcularObjetivoCercano(gps);  
        } else {
            //System.out.println("\nSe mete en objetivo fantasma");
            posicion_objetivo[0] = max_Pos / 2;
            posicion_objetivo[1] = max_Pos / 2;
        }
        
        System.out.println("Objetivo elegido");
        System.out.println(posicion_objetivo[0]+","+posicion_objetivo[1]);
        
        /*
            Con el objetivo encontrado o un objetivo imaginario:
                - Se construye una matriz que es el campo de visión del agente
            con los gradientes hacia el objetivo.
         */
        //System.out.println("\nSe le da valor a la matriz de gradientes");
        //System.out.println("matriz de gradiente");
        for (int i = 0; i < alcance; i++) {
            for (int j = 0; j < alcance; j++) {
                matrixGrad[i][j] = Math.abs(posicion_objetivo[0] - (datosGPS[0] - ((alcance - 1) / 2) + i)) + Math.abs(posicion_objetivo[1] - (datosGPS[1] - ((alcance - 1) / 2) + j));
                //System.out.print(matrixGrad[i][j] + ",");
            }
            //System.out.println("");
        }
        
        System.out.println("termina de dar valora los gradientes");
        int[] objetive = {-1, -1};
        float low_dist = (float) Math.pow(10, 10);
        int low_moving_count = -flota.get(vehiculoElegido).getPasos();

        if (pasos <= 1) {
            for (int i = 0; i < alcance; i++) {
                for (int j = 0; j < alcance; j++) {
                    if (posiblesObjetivos[i][j] == 0 && matrixGrad[i][j] < low_dist) {
                        objetive[0] = i;
                        objetive[1] = j;
                        low_dist = matrixGrad[i][j];
                    }
                }
            }
        } else if (pasos > 1) {
            for (int i = 0; i < alcance; i++) {
                for (int j = 0; j < alcance; j++) {
                    int a = datosGPS[0] - ((alcance - 1) / 2) + i;
                    int b = datosGPS[1] - ((alcance - 1) / 2) + j;
                    if (posiblesObjetivos[i][j] == 0) {
                        int casilla = matriz.getLocalMatrix()[a][b];
                        //int casilla = combinada[a][b];
                        if (flota.get(nombre).getRadar()[i][j] == 3) {
                            boolean venga = true;
                            for (int k = 0; k < posAgentsEnd.size(); k++) {
                                // Tomamos las posiciones de los agentes finalizados
                                int[] pos = new int[2];
                                pos[0] = posAgentsEnd.get(k)[0];
                                pos[1] = posAgentsEnd.get(k)[1];;
                                // La posicon a la que nos vamos a mover es a y b
                                /*System.out.println("mi posición futura es: " + a+ ","+ b);
                                System.out.println("la posicion del agente terminado es: " +pos[0]+ "," + pos[1]);*/
                                if (pos[0] == a && pos[1] == b) {
                                    venga = false;
                                }
                            }
                            if (venga) {
                                /*System.out.println("supuestamente en la base de datos hay: " + global[a][b]);
                                System.out.println("llega a ver el objetivo");
                                System.out.println("en la posicon del objetivo hay: " + global[a][b]);*/
                                objetive[0] = i;
                                objetive[1] = j;
                                return objetive;
                            }

                        } else if ((casilla > low_moving_count && global[a][b] == 5 && global[a][b] != 4)
                                || (casilla >= low_moving_count && matrixGrad[i][j] < low_dist && global[a][b] != 4)) {
                            //if (global[a][b] == 5) {
                            low_moving_count = casilla;
                            low_dist = matrixGrad[i][j];
                            objetive[0] = i;
                            objetive[1] = j;
                        }
                    }
                }
            }
            if (objetive[0] == -1) {
                System.out.println("\n\n\n\nSin direccion");
            }
        }
        return objetive;
    }

    /**
     * *************************************************************************
     * @author Raúl López Arévalo, Adrián Portillo Sánchez
     *
     * @param objetivo Recibe la diferencia entre el ID de la casilla en la que
     * se encuentra y la casilla a la que se moverá en el siguiente turno. El ID
     * 0 correspondería a la esquina superior izquierda. El ID 25
     * correspondería. a la esquina inferior derecha.
     *
     * @return mov Devuelve un String que contiene la dirección a la que
     * moverse.
     *
     */
    /*
    De esta función no hay que cambiar nada, funcionaría para cualquier tipo de matriz ya que
    lo que hace es desde la posición del agente y la siguiente casilla ID a moverse, calcular
    que dirección es. Trabaja con Id´s da igual el tamaño de la matriz.
     */
    private String pathLocalObj(int objetivo, int[][] radar) {
        camino.changeMap(radar);
        //System.out.println("TAMAÑO DEL MAPA > " + camino.getSizeMap());
        int[] diff_ids = {
            camino.getSizeMap(),
            camino.getSizeMap() - 1,
            camino.getSizeMap() + 1,
            -1,
            +1,
            -camino.getSizeMap() + 1,
            -camino.getSizeMap() - 1
        };
        String mov;

        if (objetivo == diff_ids[0]) {
            mov = "moveN";
        } else if (objetivo == diff_ids[1]) {
            mov = "moveNE";
        } else if (objetivo == diff_ids[2]) {
            mov = "moveNW";
        } else if (objetivo == diff_ids[3]) {
            mov = "moveE";
        } else if (objetivo == diff_ids[4]) {
            mov = "moveW";
        } else if (objetivo == diff_ids[5]) {
            mov = "moveSW";
        } else if (objetivo == diff_ids[6]) {
            mov = "moveSE";
        } else {
            mov = "moveS";
        }
        return mov;
    }

    /**
     * Recibimos y procesamos la percepción que va obteniendo cada vehiculo.
     *
     * @author Luis Gallego Quero
     */
    private void fasePercibir() {
        System.out.println("\n\tFASE PERCIBIR.");
        enviarMensaje(vehiculoElegido, ACLMessage.QUERY_REF, "");
        try {
            ACLMessage mensaje = receiveACLMessage();
            System.out.println(mensaje.getPerformative() + ": " + mensaje.getContent());
            if (mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                String nombreVehiculo = mensaje.getSender().name;
                PropiedadesVehicle propiedades = flota.get(nombreVehiculo);

                Percepcion percepcion = JSON.getPercepcion(mensaje.getContent());
                percepcion.setNombreVehicle(nombreVehiculo);
                //System.out.println("GPS X: " + percepcion.getGps().getPosX() + " Y: " + percepcion.getGps().getPosY());
                propiedades.actualizarPercepcion(percepcion);
                flota.put(nombreVehiculo, propiedades);
                
                int alcance = propiedades.getRol().getAlcance();
                int[][] radar = propiedades.getRadar();
                int[] gps = propiedades.getGps();
                for(int i = 0 ; i < alcance ; i++ ){
                    for(int j = 0 ; j < alcance ; j++ ){        
                        if(radar[i][j] == 3){
                            int a = gps[0] - ((alcance - 1) / 2) + i;
                            int b = gps[1] - ((alcance - 1) / 2) + j;
                            objetivos.add(new int[]{a,b});
                        }
                    }
                }
                
                Knowledge.getDB(this.MAPA).setAgentPosition(nombreVehiculo, gps[0], gps[1]);
                        
                /* Entiendo que el "goal" del "result" te indica si está en 
                el objetivo o nó, si es asi, en ese caso el x e y se corresponde
                con el punto objetivo. Supongo que será así.
                 */
                //Knowledge.getDB(this.MAPA).drawMap(); 
                if (Knowledge.getDB(this.MAPA).contains(Knowledge.STATE_GOAL)) {
                    // estadoActual = Estado.OBJETIVO_ENCONTRADO;
                }
                if (estadoActual == Estado.BUSCAR && percepcion.getLlegado()) {
                    //  objetivoEncontrado = percepcion.getLlegado();
                    // puntoObjetivo = percepcion.getGps();
                    //objetivoEncontrado();
                    // estadoActual = Estado.OBJETIVO_ENCONTRADO;
                }
                System.out.println("FIN FASE PERCIBIR.");
            }
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            estadoActual = Estado.FINALIZAR;
        }
        // subEstadoBuscando = Estado.ELECCION_VEHICULO;
        // subEstadoEncontrado = Estado.REPOSTAR;
    }

    /**
     * Fase de repostaje.
     *
     * @author Luis Gallego Quero
     */
    private void faseRepostar() {
        /*
        Inicialmente la he dejado que repueste siempre, es decir, con 
        el paso de mensaje de repostar. Habría que hacerle una pequeña
        heuristica de cuando si y cuando no repostar.
         */
        System.out.println("\n\tFASE REPOSTAR.");
        // subEstadoBuscando = Estado.MOVER;
        // subEstadoEncontrado = Estado.MOVER;
        PropiedadesVehicle propiedades = flota.get(vehiculoElegido);                    //????¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿¿
        if (!propiedades.getLlegado()) { // Repostamos si no estamos en objetivo.
            /*
            Añadir condiciones? En plan, no repostar si esta demasiado
            lejos como para conseguir llegar con la energia restante, nose..
             */
            // if(propiedades.getBateria() <= propiedades.getRol().getConsumo()) {
            enviarMensaje(vehiculoElegido, ACLMessage.REQUEST, JSON.repostar());
            try {
                ACLMessage mensaje = receiveACLMessage();
                if (mensaje.getPerformativeInt() != ACLMessage.INFORM) {
                    //System.out.println(mensaje.getPerformativeInt() + ": " + mensaje.getContent());
                    //  subEstadoBuscando = Estado.MOVER;
                    //  subEstadoEncontrado = Estado.ELECCION_VEHICULO;
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
        //System.out.println("FIN FASE REPOSTAR.");

        /*if(cont == 3 && !cont2) {
                //System.out.println("Entra cont == 3.");
                cont2 = true;
                estadoActual = Estado.BUSCAR;
                subEstadoBuscando = Estado.OBJETIVO_ENCONTRADO;
        }*/
        if (cont == 4) {
            //System.out.println("Entra cont == 5.");
            //  estadoActual = Estado.FINALIZAR;
            //faseFinalizar();
        }
    }

    /**
     * Fase objetivo encontrado.
     *
     * @author Luis Gallego Quero
     */
    private void faseObjetivoEncontrado() {
        //System.out.println("\n\tFASE OBJETIVO ENCONTRADO.");
        if (buscando) {
            buscando = false;
            estadoActual = Estado.OBJETIVO_ENCONTRADO;
            subEstadoEncontrado = Estado.ELECCION_VEHICULO;
        }
    }

    /**private void objetivoEncontrado() {
        subEstadoBuscando = Estado.OBJETIVO_ENCONTRADO;
    }*/
    private void resultadoTraza(ACLMessage msjEntrada) throws FileNotFoundException, IOException {
        //System.out.println("Recibiendo respuesta traza.");

        JsonObject injson = Json.parse(msjEntrada.getContent()).asObject();

        //System.out.println("Esto es lo que contiene la traza en json: " + injson);
        JsonArray ja = injson.get("trace").asArray();
        byte data[] = new byte[ja.size()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ja.get(i).asInt();
        }
        FileOutputStream fos = new FileOutputStream("traza_" + this.mundo + ".png");
        fos.write(data);
        fos.close();
        //System.out.println("Traza Guardada");

    }
    
    /**
     * Recopilamos los datos de los vehiculos para mandarlo a la interfaz.
     *
     * @author German Valdearenas Jimenez, Regina Lucia Alguacil Camarero
     */
    private void monitorizarVehiculos(){
        
        ArrayList<String> valores = new ArrayList();
        int[] coord = new int[2];
        
        /*Lectura del vehiculo0*/
        valores.add("Vehiculo0");
        PropiedadesVehicle p = flota.get("Vehiculo0");
        valores.add(String.valueOf(p.getBateria()));
        coord = p.getGps();
        valores.add("X["+coord[0] + "].Y[" + coord[1]+"]");
        valores.add(p.getRol().name());
        valores.add(String.valueOf(p.getRol().getAlcance()));
        valores.add(String.valueOf(p.getRol().getConsumo()));
        valores.add(String.valueOf(p.getPasos()));
                
        /*Lectura del vehiculo1*/
        valores.add("Vehiculo1");
        p = flota.get("Vehiculo1");
        valores.add(String.valueOf(p.getBateria()));
        coord = p.getGps();
        valores.add("X["+coord[0] + "].Y[" + coord[1]+"]");
        valores.add(p.getRol().name());
        valores.add(String.valueOf(p.getRol().getAlcance()));
        valores.add(String.valueOf(p.getRol().getConsumo()));
        valores.add(String.valueOf(p.getPasos()));
        
        /*Lectura del vehiculo2*/
        valores.add("Vehiculo2");
        p = flota.get("Vehiculo2");
        valores.add(String.valueOf(p.getBateria()));
        coord = p.getGps();
        valores.add("X["+coord[0] + "].Y[" + coord[1]+"]");
        valores.add(p.getRol().name());
        valores.add(String.valueOf(p.getRol().getAlcance()));
        valores.add(String.valueOf(p.getRol().getConsumo()));
        valores.add(String.valueOf(p.getPasos()));
        
        /*Lectura del vehiculo3*/
        valores.add("Vehiculo3");
        p = flota.get("Vehiculo3");
        valores.add(String.valueOf(p.getBateria()));
        coord = p.getGps();
        valores.add("X["+coord[0] + "].Y[" + coord[1]+"]");
        valores.add(p.getRol().name());
        valores.add(String.valueOf(p.getRol().getAlcance()));
        valores.add(String.valueOf(p.getRol().getConsumo()));
        valores.add(String.valueOf(p.getPasos()));
        
        miVentana.setDatosVehiculos(valores);
        
    }

}
