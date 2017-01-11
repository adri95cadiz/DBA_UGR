package gugelcar;

/*package practica3;

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

/**
 * Esta clase contiene el agente que realizara toda la funcionalidad
 *
 * @author Samuel Peregrina
 * @author Luis Gallego 
 * @version 1.0
 */
public class Controlador_Antiguo extends SingleAgent {

    // Diferentes estados del bot
    private final int LOGIN = 0;
    private final int RECIBIR_DATOS = 1;
    private final int ACCION = 2; //Mover
    private final int REPOSTAR = 3;
    private final int FINAL = 4; //Logout

    private ACLMessage msjSalida, msjEntrada;
    private boolean login;
    private boolean fin;
    private int estadoActual;
    private int nivelBateria = 0;
    private int[] datosGPS = new int[2];
    private int[][] datosRadar = new int[5][5];
    private float[][] datosScanner = new float[5][5];
    private int[][] posiblesObjetivos = new int[5][5];
    private int contadorPasos;
    private ArrayList<Integer> path_local = new ArrayList<>();
    private boolean no_exist_path = true;
    Path camino = new Path(posiblesObjetivos, 12, 12);

    // Valores modificables según que comportamiento del agente deseamos
    private boolean check = true;
    private final int MAPA =11;
    private final int LIMITE_PASOS = 1000;
    private final boolean EXPLORAR = false;

    // base de datos
    Knowledge bd = Knowledge.getDB(this.MAPA);
    JsonObject radar, gps; // toman valor en resultadoAccion() para usarlos en mapa.update

    /**
     * @autor @author Luis Gallego Germán Valdearenas Jiménez
     * @param aid
     * @throws Exception 
     */
    public Controlador_Antiguo(AgentID aid) throws Exception {
        super(aid);
    }

    /**
     * Inicializamos variables
     *
     * @author Luis Gallego Germán Valdearenas Jiménez
     */
    @Override
    public void init() {
        System.out.println("Iniciando GugelCar.");
        estadoActual = LOGIN;
        msjSalida = null;
        msjEntrada = null;
        fin = false;
        contadorPasos = 0;
    }

    /**
     * Metodo que ejecuta el agente donde controlamos los estados
     *
     * @author Luis Gallego Germán Valdearenas Jiménez
     */
    @Override
    public void execute() {
        System.out.println("Agente activo.");
        while (!fin) {
            switch (estadoActual) {
                case LOGIN:
                    realizarLogin();
                    break;
                case RECIBIR_DATOS:
                    resultadoAccion();
                    break;
                case ACCION:
                    heuristic();
                    break;
                case REPOSTAR:
                    repostar();
                    break;
                case FINAL:
                    objetivoEncontrado();
                    fin = true;
                    break;
            }
        }

    }

    /**
     * Metodo con el que se cierra sesion.
     *
     * @author Luis Gallego Germán Valdearenas Jiménez
     *
     */
    @Override
    public void finalize() {
        System.out.println("Gugelcar(" + this.getName() + ") Terminando");
        //bd.drawMap();
        super.finalize();
    }
    /**
     * @author Luis Gallego Germán Valdearenas Jiménez
     * 
     * Realiza el login del agente
     */
    private void realizarLogin() {
        System.out.println("Enviando login.");
        realizarAccion(JSON.realizarLogin(this.MAPA));
        login = true;
        //System.out.println("Pasamos a recibir datos login");
        estadoActual = RECIBIR_DATOS;
    }
    /**
     * @author Luis Gallego Germán Valdearenas Jiménez
     * Manda al servidor la acción a realizar
     * @param accion Accion realizada por el agente
     */
    private void realizarAccion(String accion) {
        System.out.println("Enviando accion " + accion + " al servidor");
        msjSalida = new ACLMessage();
        msjSalida.setSender(this.getAid());
        msjSalida.setReceiver(new AgentID("Haldus"));
        msjSalida.setContent(accion);
        this.send(msjSalida);
        System.out.println("Accion enviada.");
        //estadoActual = RECIBIR_DATOS;
    }
    /**
     * @author Luis Gallego Germán Valdearenas Jiménez
     * Recibe el resultado de la accion realizada
     */
    private void resultadoAccion() {
        System.out.println("Recibiendo respuesta.");
        boolean resultado = true;
        for (int i = 0; i < 4 && resultado; i++) {
            try {
                //System.out.println("Recibiendo respuesta2.");
                msjEntrada = receiveACLMessage();
                String recibido = msjEntrada.getContent();
                //System.out.println("Respuesta recibida. " + recibido + "\n\n");
                if (recibido.contains("scanner")) {
                    datosScanner = JSON.leerScanner(recibido);
                } else if (recibido.contains("radar")) {
                    this.radar = Json.parse(recibido).asObject();
                    datosRadar = JSON.leerRadar(recibido);
                } else if (recibido.contains("gps")) {
                    this.gps = Json.parse(recibido).asObject();
                    datosGPS = JSON.leerGPS(recibido);
                } else if (recibido.contains("trace")) {
                    System.out.println("Traza recibida");
                    this.resultadoTraza();
                } else if (login) {
                    System.out.println("ResultadoLogin: ");
                    resultado = JSON.resultadoLogin(recibido);
                    login = false;
                } else {
                    resultado = JSON.resultadoAccion(recibido);
                }

            } catch (InterruptedException ex) {
                System.err.println("Error de comunicación");
            }
        }

        if (!resultado) {
            System.out.println("La conexión ha fallado.");
            estadoActual = FINAL;
        } else if (datosRadar[2][2] == 2) {
            estadoActual = FINAL;
        } else if (nivelBateria < 50) {
            estadoActual = REPOSTAR;
        } else {
            estadoActual = ACCION;
        }
    }

    /**
     * *************************************************************************
     * @author Raúl López Arévalo Luis Gallego
     *
     * Muestra que la traza se ha guardado correctamente mediante un mensaje por
     * pantalla y guarda la misma en un archivo .png con el nombre
     * correspondiente al mapa que es.
     *
     */
    private void resultadoTraza() {
        System.out.println("Recibiendo respuesta traza.");
        try {
            msjEntrada = receiveACLMessage();
            JsonObject injson = Json.parse(msjEntrada.getContent()).asObject();
            JsonArray ja = injson.get("trace").asArray();
            byte data[] = new byte[ja.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) ja.get(i).asInt();
            }
            FileOutputStream fos = new FileOutputStream("traza_" + this.MAPA + ".png");
            fos.write(data);
            fos.close();
            System.out.println("Traza Guardada");
        } catch (InterruptedException | IOException ex) {
            System.err.println("Error de comunicación");
        }
    }

    /**
     * *************************************************************************
     * @author Raúl López Arévalo
     *
     * Método que actualiza el mapa desde la base de datos
     */
    private void updateMap() {
        bd.updateStatus(radar, gps, contadorPasos);
    }

    /**
     * Pone los objetivos inaccesibles del 5x5 que rodea al gugelcar a -1 y los
     * accesibles a 0.
     *
     * @author Adrian Portillo Sanchez
     */
    private void eliminarObjetivosInaccesibles() {
        eliminarObjetivosInaccesiblesRec(2, 2);
        posiblesObjetivos[2][2] = -1;		//Pone la posicion actual a -1, no se deberia deliberar sobre ella.
        for (int i = 0; i < 5; i++) {		//Pone los objetivos no alcanzados a -1, tampoco son accesibles.
            for (int j = 0; j < 5; j++) {
                if (posiblesObjetivos[i][j] == 0) {
                    posiblesObjetivos[i][j] = -1;
                }
            }
        }
        for (int i = 0; i < 5; i++) {		//Pone los 1's a 0's dejando finalmente los accesibles a 0 y los inaccesibles a -1.
            for (int j = 0; j < 5; j++) {
                if (posiblesObjetivos[i][j] == 1) {
                    posiblesObjetivos[i][j] = 0;
                }
            }
        }
    }

    /**
     * Funcion recursiva .
     *
     * @author Adrian Portillo Sanchez
     */
    private void eliminarObjetivosInaccesiblesRec(int row, int col) {
        if (row < 0 || row > 4 || col < 0 || col > 4) {
        } else if (posiblesObjetivos[row][col] == -1 || posiblesObjetivos[row][col] == 1) {
        } else if (datosRadar[row][col] == 1) {
            posiblesObjetivos[row][col] = -1;
        } else {
            posiblesObjetivos[row][col] = 1;
            eliminarObjetivosInaccesiblesRec(row - 1, col - 1);	//Superior izquierdo.
            eliminarObjetivosInaccesiblesRec(row - 1, col);		//Superior centro.
            eliminarObjetivosInaccesiblesRec(row - 1, col + 1);	//Superior derecho.
            eliminarObjetivosInaccesiblesRec(row, col - 1);		//Centro izquierdo.
            eliminarObjetivosInaccesiblesRec(row, col + 1);		//Centro derecho.
            eliminarObjetivosInaccesiblesRec(row + 1, col - 1);	//Inferior izquierdo.
            eliminarObjetivosInaccesiblesRec(row + 1, col);		//Inferior centro.
            eliminarObjetivosInaccesiblesRec(row + 1, col + 1);	//Inferior derecho.	
        }
    }

    /**
     * *************************************************************************
     * @author Raúl López Arévalo
     *
     * Determina la mejor posición a la que moverse dentro del mapa local 5x5.
     * En el primer movimiento decide la casilla objetivo teniendo en cuenta si
     * es accesible y la cercanía al objetivo.
     *
     * A partir del primer movimiento para decidir esta casilla tiene en cuenta
     * el mapa de la base de datos.
     *
     * @return objetivo Devuelve las coordenadas de la casilla como int[].
     */
    private int[] chooseLocalObj() {
        int[] objetive = new int[2];
        Arrays.deepToString(posiblesObjetivos);
        if (contadorPasos <= 0) { //cambiar a 1
            float low_dist = (float) Math.pow(10, 10);

            for (int i = 1; i <= 3; i++) {
                for (int j = 1; j <= 3; j++) {
                    //System.out.println("Posible objetivo: " + posiblesObjetivos[i][j]);
                    //System.out.println("Distancia menor: " + low_dist + "  datosScanner: " + datosScanner[i][j]);
                    if (posiblesObjetivos[i][j] == 0 && datosScanner[i][j] < low_dist) {
                        low_dist = datosScanner[i][j];
                        objetive[0] = i;
                        objetive[1] = j;
                        //System.out.println("Encontrado nuevo objetivo mejor para moverse");
                    }
                }
            }
        }
        //System.out.println("DATOS GPS :  " + datosGPS[0] + "," + datosGPS[1]);

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                //System.out.print(posiblesObjetivos[i][j] + "-");
            }
            //System.out.println("\n");
        }
        //System.out.println("");

        if (contadorPasos > 0) {

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
            
            for (int i = 0; i <= 4; i++) {
                for (int j = 0; j <= 4; j++) {
                    int a = datosGPS[0] - 2 + i;
                    int b = datosGPS[1] - 2 + j;
                    //System.out.println("\t\t\tDatos del gps: " + datosGPS[0] + "," + datosGPS[1]);
                    //System.out.println("\t\t\tVamos a acceder a: " + a + "," + b);
                    //System.out.println("\t\t\tPosibles accesibles: " + Arrays.deepToString(posiblesObjetivos));

                    // Comrueba que no se esté accediendo a una posición inválida de la matriz de la BD.
                    if (a >= 0 && b >= 0 && a < bd.getMatrixSize() && b < bd.getMatrixSize()) {
                        //System.out.println("Entra primero");
                        if (posiblesObjetivos[i][j] == 0) {
                            if (bd.getStatus(a, b) < low_moving_count || (bd.getStatus(a, b) == low_moving_count && datosScanner[i][j] < low_dist2)){//&& datosScanner[i][j] < low_dist2){
                                //|| (bd.getStatus(a, b) == low_moving_count && datosScanner[i][j] < low_dist2)) {
                                System.out.print(i+","+j+": "+bd.getStatus(a,b)+" - ");
                                System.out.print(i+","+j+": "+posiblesObjetivos[i][j]+" | ");
                                //if (posiblesObjetivos[i][j] == 0) {
                                //System.out.println("Entra despues");
                                low_moving_count = bd.getStatus(a, b);
                                //System.out.println(low_moving_count);
                                low_dist2 = datosScanner[i][j];
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
     * @author Raúl López Arévalo
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
    private String pathLocalObj(int objetivo) {
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
     * Estado repostar.
     *
     * @author Luis Gallego
     */
    private void repostar() {
        System.out.println("Respostando.");
        realizarAccion(JSON.realizarAccion("refuel"));
        nivelBateria = 100;
        estadoActual = RECIBIR_DATOS;
    }

    /**
     * *************************************************************************
     * @author Raúl López Arévalo
     *
     *
     *
     */
    private void heuristic() {
        // Control sobre la batería
        if (nivelBateria < 1) {
            repostar();
        }
        // Limitar número de pasos
        if (contadorPasos == LIMITE_PASOS) {
            estadoActual = FINAL;
        } else {
            // Si en la matriz de conocimiento está guardado el objetivo,
            // no se ha comprobado antes, 
            // y la opcion explorar está desactivada: 
            // El agente encontrará un camino optimo desde donde empieza hasta el objetivo
            // pasando por zonas que ya ha explorado anteriormente (otras sesiones)
            if (bd.contains(-2) && !EXPLORAR) {
                System.out.println("ESTE MAPA YA TIENE GUARDADO EL OBJETIVO O PARTE DE EL");
                check = false;
                int tamMap = bd.tamMap();
                System.out.println("ID DEL OBJETIVO -----------------> " + bd.getIDValue(-2));
                System.out.println("TAMAÑO DEL MAPA -----------------> " + tamMap);
                System.out.println("COORDENADAS AGENTE --------------> " + datosGPS[0] + datosGPS[1]);

                System.out.println("ACCEDIENDO A CAMBIAR LA POSICON INICIAL");
                camino.chageStarte(datosGPS[0] * tamMap + datosGPS[1]);
                System.out.println("OK");
                System.out.println("ACCEDIENDO A CAMBIAR EL OBJETIVO");
                camino.changeObjetive(bd.getIDValue(-2));
                System.out.println("OK");
                System.out.println("ACCEDIENDO A CAMBIAR LA MATRIZ");
                camino.changeMap(bd.getMap());
                System.out.println("OK");
                System.out.println("CALCULANDO EL PATH");
                path_local = camino.getPath();
                System.out.println("OK");
                System.out.println("ESTE ES EL CAMINO A SEGUIR");
                camino.printPath();
                System.out.println("MATRIZ OPTIMA ----------------------------->");
                bd.printoptim();
                no_exist_path = false;

            }
           if (path_local.contains(-1)) {
                fin = true;
                System.out.println("OBJETIVO INACCESIBLE");
                realizarAccion(JSON.realizarAccion("logout"));
                resultadoAccion();
                resultadoTraza();
                System.out.println("Pasos: " + contadorPasos);
            } else {
            // Si no existe ningún camino óptimo local a seguir, se calcula otro
            if (no_exist_path) {
                /**
                 * -Calcula los objetivos no accesibles por el agente
                 * -De entre los válidos elige una casilla hacia la que moverse
                 * -Se crea un path óptimo desde el agente hasta dicha casilla
                 */
                path_local.clear();
                posiblesObjetivos = new int[5][5];
                eliminarObjetivosInaccesibles();
                //System.out.println("Posibles Objetivos: " + Arrays.deepToString(posiblesObjetivos));

                    // Se decide la casilla óptima a moverse en la matriz 5x5
                    int[] objetivo_alcanzar = chooseLocalObj();
                    int objetivo_id = objetivo_alcanzar[0] * camino.getSizeMap() + objetivo_alcanzar[1];
                    // Se calcula el camino optimo para llegar hasta ella
/*
                System.out.println("RADAR ------------------> ");
                for (int i = 0; i < 5; i++) {
                    for (int j = 0; j < 5; j++) {
                        System.out.print(datosRadar[i][j] + "-");
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
                System.out.println(objetivo_id);
*/
                // Actualizar el objeto camino con el nuevo radar y la nueva pos final
                camino.changeMap(posiblesObjetivos);
                camino.changeObjetive(objetivo_id);
                camino.chageStarte(12);
                //System.out.println("CAMBIOS REALIZADOS, OBJETIVO Y MATRIZ");
                //System.out.println("NUEVO PATH OBTENIDO");
                path_local = camino.getPath();
//                //System.out.println("COORDENADAS DE TODO EL PATH --------------> ");
//                for (int i = 0; i < path_local.size(); i++) {
//                    System.out.print(path_local.get(i) / 5 + "-" + path_local.get(i) % 5 + " ");
//                }
//                System.out.println("\nCaminito pintao");
            }
            /**
             * Si ya existía un path óptimo: -Calcula la posición a la que se
             * debe de mover según el path -Elimina dicha posición del path -Una
             * vez que se haya completado el path se vuelve a calcular uno
             */
            no_exist_path = false;
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
            String movimiento = pathLocalObj(obj_prox_mov);
            
            path_local.remove(0);
            // Si el path_local restante contiene solamente una posición, es la 
            // del propio agente por lo que se borra.
            if (path_local.size() == 1) {
                no_exist_path = true;
            }
            /****************
             * Una vez se sabe en que dirección se quiere mover:
             * -Actualiza el mapa de la base de datos
             * -Control sobre número de pasos y batería
             * -Manda la acción del movimiento
             * -Espera a recibir la respuesta
             * */
            
            updateMap();
            //System.out.println("Datos del GPS bien puestos: " + datosGPS[0] + datosGPS[1] + "\n\t\tPaso numero: " + this.contadorPasos + "\n");
            contadorPasos++;
            nivelBateria--;
            realizarAccion(JSON.realizarAccion(movimiento));
            System.out.println("\t\tPaso numero----------> " + contadorPasos);
            estadoActual = RECIBIR_DATOS;

        }
        bd.drawMap();
        }
    }

    /**
     * Estado final.
     *
     * @author Luis Gallego
     */
    private void objetivoEncontrado() {
        System.out.println("Objetivo encontrado.");
        realizarAccion(JSON.realizarAccion("logout"));
        resultadoAccion();
        resultadoTraza();
        System.out.println("Pasos: " + contadorPasos);
        fin = true;
    }

}
