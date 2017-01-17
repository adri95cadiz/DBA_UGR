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

/**
 *
 * @author Luis Gallego Quero
 */
public class Controlador extends SingleAgent {

    private final String NOMBRE_SERVIDOR = "Haldus";
    private final short TAM_MAPA = 500;
    private String mundo;
    private String conversationID;
    private String vehiculoElegido;
    private Estado estadoActual, subEstadoBuscando, subEstadoEncontrado;
    private boolean fin, buscando;
    private HashMap<String, PropiedadesVehicle> flota;
    //private Celda[][] mapa = new Celda[TAM_MAPA][TAM_MAPA];
    //private double[][] scanner = new double[TAM_MAPA][TAM_MAPA];   
    private boolean objetivoEncontrado;
    private Cell puntoObjetivo = new Cell();
    ArrayList<String> vehiculosExploradores = new ArrayList<>(); 
    ArrayList<String> vehiculosEsperando = new ArrayList<>();
    ArrayList<String> vehiculosFinalizados = new ArrayList<>();
    private int[][] posiblesObjetivos;
    private boolean cont2;
    boolean exist_path = false;
    Path camino;
    int cont;
    private ArrayList<Integer> path_local = new ArrayList<>();
    // Valores modificables según que comportamiento del agene deseamos    
    private final int MAPA =11; 
    private boolean check = true;
    private final int LIMITE_PASOS = 1000;
    private final boolean EXPLORAR = false;

    public Controlador(AgentID id, String mundo) throws Exception {
        super(id);
        this.mundo = mundo;
    }

    public void init() {
        System.out.println("Iniciandose Controlador " + getName());
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
        while (!fin) {
            /*
            En faseRepostar() hay un par de condiciones para que la prueba
            inicial no se alargue hasta el infinito. Las he puesto ahí ya que
            es el último paso del ciclo de paso de mensajes y así no
            perdemos mensajes en el cambio de estados.
            Si al ejecutar veis errores posiblemente sean porque el 
            coche se ha estrellado.
             */
            System.out.println("Execute fin: " + fin + "\nFase: " + estadoActual);
            switch (estadoActual) {
                case INICIAL:
                    faseInicial();
                    break;
                case BUSCAR:
                    switch (subEstadoBuscando) {
                        case ELECCION_VEHICULO:
                            faseEleccionVehiculo();
                            if (vehiculoElegido.equals("")) {
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
                            if (vehiculoElegido.equals("")) {
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
     *
     * @param receptor Agente.
     * @param performativa Performativa.
     * @param contenido Cadena a enviar.
     * @author Luis Gallego Quero.
     */
    private void enviarMensaje(String receptor, int performativa, String contenido) {
        System.out.println("\nEn el enviar Mensaje del controlador: \n");
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
     *
     * @author Luis Gallego Quero.
     */
    private void iniciarConversacion() {
        System.out.println("\n\tINICIO CONVERSACION");
        String contenido = JSON.suscribirse(mundo);
        enviarMensaje(NOMBRE_SERVIDOR, ACLMessage.SUBSCRIBE, contenido);
        ACLMessage mensaje = null;
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

                        //System.out.println("Esto es lo que deberían de ser las propiedades: " + mensaje.getContent());
                        propiedades.setRol(JSON.getRol(mensaje.getContent()));
                        flota.put(mensaje.getSender().name, propiedades);
                    }
                }
            }
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            estadoActual = Estado.FINALIZAR;
        }
        System.out.println("\tFIN INICIO CONVERSACION");
    }

    /**
     * Inicializamos las propiedades de cada vehiculo.
     *
     * @author Luis Gallego Quero.
     */
    private void inicializarPropiedadesVehiculo() { // En realidad las propiedades del vehículo se podrían coger antes, en la inicialización
        System.out.println("\n\tINICIO PROPIEDADES VEHICULO");
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
                    flota.put(nomVehiculo, propiedades);
                    System.out.println("\nNombre vehiculo: " + nomVehiculo);
                    System.out.println("\nRol: " + propiedades.getRol());
                    System.out.println("\nNombre vehiculo" + flota.get(nomVehiculo));
                    System.out.println(mensaje.getContent());
                }
            }
        } catch (InterruptedException ex) {
            System.err.println(ex.toString());
            estadoActual = Estado.FINALIZAR;
        }
        System.out.println("\tFIN PROPIEDADES VEHICULO");
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
        // Cogemos los vehículos que sean coche, que serán los que exploren.
        // Los demás están esperando a que los exploradores lleguen al objetivo.
        for (String vehiculo : flota.keySet()) {
            if (flota.get(vehiculo).getRol().getId() == 1) {
                vehiculosExploradores.add(vehiculo);
            }else{
                vehiculosEsperando.add(vehiculo);
            }
        } // Si no hay ningún coche cogeremos los camiones
        // Los demás están esperando a que los exploradores lleguen al objetivo.
        if (vehiculosExploradores.isEmpty()) {
            for (String vehiculo : flota.keySet()) {
                if (flota.get(vehiculo).getRol().getId() == 2) {
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
     *
     * @author Luis Gallego Quero
     */
    private void finalizarConversacion() {
        for (String vehiculo : flota.keySet()) {
            enviarMensaje(vehiculo, ACLMessage.CANCEL, "");
        }
        enviarMensaje(NOMBRE_SERVIDOR, ACLMessage.CANCEL, "");
        ACLMessage mensaje = null;
        try {
            mensaje = receiveACLMessage();
            if (mensaje.getPerformativeInt() == ACLMessage.AGREE) {
                JSON.leerKey(mensaje.getContent());
            }
            // Segundo mensaje para recibir la traza. (ADAPATARLA)
            mensaje = receiveACLMessage();
            if (mensaje.getPerformativeInt() == ACLMessage.INFORM) {
                System.out.println("TRAZA: " + mensaje.getContent());
                try {
                    resultadoTraza(mensaje);
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
        if (buscando) {

            /*
            Caso en el cual aún no sabemos donde esta el punto objetivo,
            es decir, estarían todos en modo "explorador".
            ¿Utilizar solo los de menor consumo y mayor campo de vision?
             */
            vehiculoElegido = vehiculosExploradores.get(0);
            
            /***************** INFORMACION ************************************/
            PropiedadesVehicle p = flota.get(vehiculoElegido);
            Rol r = p.getRol();
            System.out.println("\nPropiedades del vehiculo elegido: ");
            System.out.println("\nBateria: " + p.getBateria());
            int[] coord = new int[2];
            coord = p.getGps();
            System.out.println("\nRadar: "+ java.util.Arrays.deepToString(p.getRadar()));
            System.out.println("\ncoordenadas: " + coord[0] + "." + coord[1]);
            System.out.println("\nRol: " + p.getRol());
            System.out.println("\nAlcance: " + r.getAlcance());
            System.out.println("\nConsumno: " + r.getConsumo());
            /******************************************************************/

        } else {
            /*
            Ya no estamos buscando por lo que conocemos el punto.  
            Movemos el resto de vehiculos que estaban parados sin explorar.
             */
            vehiculoElegido = vehiculosEsperando.get(0); // Si es que hay alguno porque pueden ser todos coche
        }

    }

    /**
     * Elegimos el movimiento mas optimo para el vehiculo seleccionado.
     */
    private void faseMover() {
        System.out.println("\n\tFASE MOVER.");
        // Utilizar diferentes funciones Mover para el caso buscar.
        if (buscando) {
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
     *
     * @author Luis Gallego Quero
     */
    private void mover() {
        /*
        Aquí se obtienen y muestran las propiedades del vehículo
        */
        System.out.println("\n======================================================================");
            PropiedadesVehicle p = flota.get(vehiculoElegido);
            Rol r = p.getRol();
            System.out.println("Propiedades del vehiculo elegido: ");
            System.out.println("\nBateria: " + p.getBateria());
            int[] coord = new int[2];
            coord = p.getGps();
            int[][] radar = p.getRadar();
            System.out.println("\nRadar: "+ java.util.Arrays.deepToString(radar));
            System.out.println("\ncoordenadas: " + coord[0] + "." + coord[1]);
            System.out.println("\nRol: " + p.getRol());
            int alcance = r.getAlcance();
            System.out.println("\nAlcance: " + alcance);
            System.out.println("\nConsumo: " + r.getConsumo());
            int pasos = p.getPasos();
            System.out.println("\nPasos dados: " + pasos);
        System.out.println("======================================================================");
        /*
        Control sobre la batería del vehículo elegido para moverse
        */
        if( flota.get(vehiculoElegido).getBateria() < 10 ){
            faseRepostar();
        }
        /*
        1 - Si el objetivo existe y hay camino, de tirón.
        2 - Si no existe un camino óptimo local, se calcula uno aquí.
        3 - Si ya existe un camino óptimo local, se van cogiendo los movimientos
        hasta que llegue a su casilla destino.
        4 - Actualizar Knowedge
        5 - Acción de mover
        */
        
        
        
        System.out.println("\n\t MOVER()");
        String decision = " ";
        cont++;
        
        
        
        /*
        Aquí se decide el movimiento
        */
        /*
        ¿Código para ver si existe camino al objetivo?
        */
        if(/*Existe camino entre vehículo y un objetivo*/false && !exist_path){       //Si no existe un camino establecido y es posible encontrar un camino explorado entre el vehículo y un objetivo
            camino = new Path(p.getMatrix().getKnowledgeMatrix(), p.getGps()[0]*alcance + p.getGps()[1], 12);
            //camino.changeObjetive(/*Objetivo que está accesible más cercano*/);            
            path_local.clear();
            path_local = camino.getPath();  
            exist_path = true;
        } else if(!exist_path){                                                 //Si no existe un camino establecido pero tampoco se conoce el espacio entre el vehículo y ningún objetivo,
            posiblesObjetivos = new int [alcance][alcance];
            eliminarObjetivosInaccesibles(radar.clone(), alcance);
            /*System.out.println("Posibles Objetivos: " + Arrays.deepToString(posiblesObjetivos));
            for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        System.out.print(posiblesObjetivos[i][j] + "-");
                    }
                    System.out.println("\n");
                }   */ 
            // Se decide la casilla óptima a moverse en la matriz 5x5
            
            int[] objetivo_alcanzar = chooseLocalObj(pasos, coord, p.getNombre(), p.getMatrix());
            int objetivo_id = objetivo_alcanzar[0]*alcance + objetivo_alcanzar[1];
            
            // Se calcula el camino optimo para llegar hasta ella

            /*System.out.println("RADAR ------------------> ");
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        System.out.print(radar[i][j] + "-");
                    }
                    System.out.println("\n");
                }
            System.out.println("");

            System.out.println("POSIBLES ------------------> ");
            for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        System.out.print(posiblesObjetivos[i][j] + "-");
                    }
                    System.out.println("\n");
                }
            System.out.println("");

            System.out.println("OBJETIVO ID ------------> ");
            System.out.println(objetivo_id);*/

            // Actualizar el objeto camino con el nuevo radar y la nueva pos final
            camino = new Path(posiblesObjetivos, 12, 12);
            camino.changeObjetive(objetivo_id);
            camino.changeStart((int)floor((alcance*alcance)/2.0));
            //System.out.println("CAMBIOS REALIZADOS, OBJETIVO Y MATRIZ");
            //System.out.println("NUEVO PATH OBTENIDO");
            path_local.clear();
            path_local = camino.getPath();
        }   
        
        /**
        * Si ya existía un path óptimo: -Calcula la posición a la que se
        * debe de mover según el path -Elimina dicha posición del path -Una
        * vez que se haya completado el path se vuelve a calcular uno
        */
        exist_path = true;
        System.out.println("CAMINO A SEGUIR:");
        camino.printPath();
        // Se transforman las IDs de las casillas a coordenadas para saber
        // identificar la dirección en la que se debe de mover
        int primera_casilla = path_local.get(0);
        int segunda_casilla = path_local.get(1);
        int obj_prox_mov = primera_casilla - segunda_casilla;
        //System.out.println("DIFERENCIA ENTRE CASILLAS --------------> " + obj_prox_mov);
        //System.out.println("COORDENADAS A MOVERSE POR EL PRIMER PATH -------------------->  " + path_local.get(1) / 5 + "," + path_local.get(1) % 5);
        //System.out.println("COORDENADAS A MOVERSE POR EL PRIMER PATH: " + obj_prox_mov[0] + obj_prox_mov[1]);*/

        // Se obtiene la dirección en la que moverse
        decision = pathLocalObj(obj_prox_mov, radar);

        /****************
         * Una vez se sabe en que dirección se quiere mover:
         * -Actualiza el mapa de la base de datos
         * -Control sobre número de pasos y batería
         * -Manda la acción del movimiento
         * -Espera a recibir la respuesta
         * */       
        if (!decision.contains("move")) { 
            System.out.println("No se donde moverme.");
            subEstadoBuscando = Estado.ELECCION_VEHICULO;
            subEstadoEncontrado = Estado.ELECCION_VEHICULO;
        } else {
            enviarMensaje(vehiculoElegido, ACLMessage.REQUEST, JSON.mover(decision));
            subEstadoBuscando = Estado.PERCIBIR;
            subEstadoEncontrado = Estado.PERCIBIR;
            try {
                ACLMessage mensaje = receiveACLMessage();
                if (mensaje.getPerformativeInt() != ACLMessage.INFORM) {
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
        path_local.remove(0);
        // Si el path_local restante contiene solamente una posición, es la 
        // del propio agente por lo que se borra.
        if (path_local.size() == 1) {
            exist_path = false;
        }
        //System.out.println("Datos del GPS bien puestos: " + datosGPS[0] + datosGPS[1] + "\n\t\tPaso numero: " + this.contadorPasos + "\n");
        p.darPaso();
        System.out.println("\t\tPaso numero: " + p.getPasos());  
    }
  
    /**
     * Pone los objetivos inaccesibles del [alcance x alcance] que rodea al gugelcar a -1 y los
     * accesibles a 0.
     *
     * @author Adrian Portillo Sanchez
     */
    private void eliminarObjetivosInaccesibles(int[][] radar, int alcance) {
        int pos_inicial = (int) floor(alcance/2.0);
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
        if (row < 0 || row > alcance-1 || col < 0 || col > alcance-1) {                             //Se encuentra fuera de los límites
        } else if (posiblesObjetivos[row][col] == -1 || posiblesObjetivos[row][col] == 1) {     //Aunque dentro de los límites ya ha sido recorrida 
        } else {          
            int pos_inicial = (int) floor(alcance/2.0);
            if ((row != pos_inicial || col != pos_inicial) && (radar[row][col] == 1 || radar[row][col] == 2 || radar[row][col] == 4)) {
                posiblesObjetivos[row][col] = -1;                                                   //Aunque alcanzable posee un obstáculo en este momento
            } else {
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
     * Determina la mejor posición a la que moverse dentro del mapa local [alcance x alcance].
     * En el primer movimiento decide la casilla objetivo teniendo en cuenta si
     * es accesible y la cercanía al objetivo.
     *
     * A partir del primer movimiento para decidir esta casilla tiene en cuenta
     * el mapa de la base de datos.
     *
     * @return objetivo Devuelve las coordenadas de la casilla como int[].
     */
    private int[] chooseLocalObj(int pasos, int[]datosGPS, String nombre, GugelVehicleMatrix matriz) {       
        int[] objetive = new int[2];
        int[][] matrixGrad = flota.get(vehiculoElegido).getRadar();
        int alcance = flota.get(vehiculoElegido).getRol().getAlcance(); 
        if (estadoActual == Estado.OBJETIVO_ENCONTRADO){
            
            int[] posicion_objetivo = new int[2]; 
            int[] gps = flota.get(vehiculoElegido).getGps();
            
            //Utilizar gradiente con varios objetivos. Falta hacer esto
            // Habría que pasarle a esta función un array list o un algo que contenga a los objetivos.
            // En caso de que no se sepa la localización de un segundo objetivo, sus coordenadas SON -1,-1.
            // El primer objetivo (objetivos[0]) siempre va a existir si entra en este if.
            int[][]objetivos = new int[2][2]; // Esto va a tener 2 coordenadas de un objetivo, y otras dos del otro objetivo. Pueden ser null.
            // Si se han encontrado 2 objetivos:
            if(objetivos[1][0] != -1){
                // Calcula el gradiente en la posición del agente para cada objetivo
                int dist1 = Math.abs(objetivos[0][0]-gps[0]) + Math.abs(objetivos[0][1]-gps[1]);
                int dist2 = Math.abs(objetivos[1][0]-gps[0]) + Math.abs(objetivos[1][1]-gps[1]);
                // Si el objetivo 1 está más cerca lo cogemos como destino.
                if( dist1 < dist2 ){
                    posicion_objetivo[0] = objetivos[0][0];
                    posicion_objetivo[1] = objetivos[0][1];
                }else{ // si no, cogemos el objetivo 2
                    posicion_objetivo[0] = objetivos[1][0];
                    posicion_objetivo[1] = objetivos[1][1];
                }
            }else{ // Si solo hay un objetivo, tomamos ese como destino
                posicion_objetivo[0] = objetivos[0][0];
                posicion_objetivo[1] = objetivos[0][1];
            }
            /*
            Primer bloque si el objetivo se ha encontrado:
                - Se construye una matriz que es el campo de visión del agente
            con los gradientes hacia el objetivo.
            */
            
            for(int i=0; i<alcance; i++){
                for(int j=0; j<alcance; j++){
                    matrixGrad[i][j] = Math.abs(posicion_objetivo[0]-i) - Math.abs(posicion_objetivo[1]-j);
                }
            }
        } else {
            for(int i=0; i<alcance; i++){
                for(int j=0; j<alcance; j++){
                    matrixGrad[i][j] = i+j;
                }
            }
            int pos_inicial = (int) floor(alcance/2.0);
            matrixGrad[pos_inicial][pos_inicial] = 100;
        }
        if (pasos <= 1) { 
            float low_dist = (float) Math.pow(10, 10);

            for (int i = 0; i <= alcance-1; i++) {
                for (int j = 0; j <= alcance-1; j++) {
                    //System.out.println("Posible objetivo: " + posiblesObjetivos[i][j]);
                    //System.out.println("Distancia menor: " + low_dist + "  datosScanner: " + datosScanner[i][j]);
                    if (posiblesObjetivos[i][j] == 0 && matrixGrad[i][j] < low_dist) {
                        low_dist = matrixGrad[i][j];
                        objetive[0] = i;
                        objetive[1] = j;
                        //System.out.println("Encontrado nuevo objetivo mejor para moverse");
                    }
                }
            }
        }
        //System.out.println("DATOS GPS :  " + datosGPS[0] + "," + datosGPS[1]);

        /*for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                //System.out.print(posiblesObjetivos[i][j] + "-");
            }
            //System.out.println("\n");
        }*/
        //System.out.println("");

        if (pasos > 0) {

            float low_dist2 = (float) Math.pow(10, 10);
            int low_moving_count = (int) Math.pow(10, 10);
            //System.out.println("\t\t\tExplorando el mapa");
            
//            System.out.println("Vision del agente");
//            for (int i = 0; i <= 4; i++) {
//                for (int j = 0; j <= 4; j++) {
//                    System.out.print(posiblesObjetivos[i][j]+" ");
//                }System.out.println("\n");
//            }
//            
//            System.out.println("Vision de matriz");
//            for (int i = 0; i <= 4; i++) {
//                for (int j = 0; j <= 4; j++) {
//                    int a = datosGPS[0] - 2 + i;
//                    int b = datosGPS[1] - 2 + j;
//                    System.out.print(bd.getStatus(a,b)+" ");
//                }System.out.println("\n");
//            }
            
            for (int i = 0; i <= alcance-1; i++) {
                for (int j = 0; j <= alcance-1; j++) {
                    int a = datosGPS[0] - 2 + i;
                    int b = datosGPS[1] - 2 + j;
                    //System.out.println("\t\t\tDatos del gps: " + datosGPS[0] + "," + datosGPS[1]);
                    //System.out.println("\t\t\tVamos a acceder a: " + a + "," + b);
                    //System.out.println("\t\t\tPosibles accesibles: " + Arrays.deepToString(posiblesObjetivos));

                    // Comprueba que no se esté accediendo a una posición inválida de la matriz de la BD.
                    if (a >= 0 && b >= 0 && a < Knowledge.getDB(this.MAPA).mapSize() && b < Knowledge.getDB(this.MAPA).mapSize()) {
                        //System.out.println("Entra primero");
                        if (posiblesObjetivos[i][j] == 0) {
                            int casilla = matriz.getCombinedKnowledge(nombre)[i][j];
                            if (casilla < low_moving_count || (casilla == low_moving_count && matrixGrad[i][j] < low_dist2)){
                                //|| (bd.getStatus(a, b) == low_moving_count && datosScanner[i][j] < low_dist2)) {
                                System.out.print(i+","+j+": "+casilla+" - ");
                                System.out.print(i+","+j+": "+posiblesObjetivos[i][j]+" | ");
                                //if (posiblesObjetivos[i][j] == 0) {
                                //System.out.println("Entra despues");
                                low_dist2 = matrixGrad[i][j];
                                low_moving_count = casilla;
                                objetive[0] = i;
                                objetive[1] = j;
                                //System.out.println("NUEVO OBJETIVO EXPERIMENTAL ENCONTRADO ----> " + objetive[0] + "," + objetive[1]);
                            }
                        }
                    }
                }
                System.out.println("\n");
            }
            //System.out.println("\n\n\t\tObjetivo experimental para moverse: " + objetive[0] + objetive[1]);
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
        System.out.println("TAMAÑO DEL MAPA > " + camino.getSizeMap());
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
                propiedades.actualizarPercepcion(percepcion);
                flota.put(nombreVehiculo, propiedades);
                propiedades.updateMatrix();
                /* Entiendo que el "goal" del "result" te indica si está en 
                el objetivo o nó, si es asi, en ese caso el x e y se corresponde
                con el punto objetivo. Supongo que será así.
                 */
                Knowledge.getDB(this.MAPA).drawMap(); 
                if(Knowledge.getDB(this.MAPA).contains(Knowledge.STATE_GOAL)){
                    estadoActual = Estado.OBJETIVO_ENCONTRADO;
                }
                if (estadoActual == Estado.BUSCAR && percepcion.getLlegado()) {
                    objetivoEncontrado = percepcion.getLlegado();
                    puntoObjetivo = percepcion.getGps();
                    //objetivoEncontrado();
                    estadoActual = Estado.OBJETIVO_ENCONTRADO;
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
        subEstadoBuscando = Estado.MOVER;
        subEstadoEncontrado = Estado.MOVER;
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
        /*if (cont == 5) {
            System.out.println("Entra cont == 5.");
            estadoActual = Estado.FINALIZAR;
        }*/
    }

    /**
     * Fase objetivo encontrado.
     *
     * @author Luis Gallego Quero
     */
    private void faseObjetivoEncontrado() {
        System.out.println("\n\tFASE OBJETIVO ENCONTRADO.");
        if (buscando) {
            buscando = false;
            estadoActual = Estado.OBJETIVO_ENCONTRADO;
            subEstadoEncontrado = Estado.ELECCION_VEHICULO;
        }
    }

    /*private void objetivoEncontrado() {
        subEstadoBuscando = Estado.OBJETIVO_ENCONTRADO;
    }*/
    private void resultadoTraza(ACLMessage msjEntrada) throws FileNotFoundException, IOException {
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
