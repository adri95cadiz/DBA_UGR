package gugelcar;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import edu.emory.mathcs.backport.java.util.Arrays;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Esta clase contiene el agente que realizara toda la funcionalidad
 *
 * @author Samuel Peregrina
 * @author Luis Gallego
 * @version 1.0
 */
public class AgentCar extends SingleAgent {

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
    private int nivelBateria = 100;
    private int[] datosGPS = new int[2];
    private int[][] datosRadar = new int[5][5];
    private float[][] datosScanner = new float[5][5];
    private int[][] posiblesObjetivos = new int[5][5];
    private int contadorPasos;
    private ArrayList<Integer> datosTraza = new ArrayList<>();
    private ArrayList<Integer> path_local = new ArrayList<>();
    private boolean camino_aun = false;
    Path camino = new Path(posiblesObjetivos, 12, 12);
    
    private final int MAPA = 2;
    private final int LIMITE_PASOS = 70;

    // base de datos
    Knowledge bd = Knowledge.getDB(this.MAPA);
    int[][] mapa;
    JsonObject radar, gps; // toman valor en resultadoAccion() para usarlos en mapa.update

    public AgentCar(AgentID aid) throws Exception {
        super(aid);
    }

    /**
     * Inicializamos variables
     *
     * @author Luis Gallego
     */
    @Override
    public void init() {
        System.out.println("Iniciando GugelCar.");
        estadoActual = LOGIN;
        msjSalida = null;
        msjEntrada = null;
        fin = false;
        contadorPasos = 1;
    }

    /**
     * Metodo que ejecuta el agente donde controlamos los estados
     *
     * @author Luis Gallego
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
                    mover();
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
     */
    @Override
    public void finalize() {
        System.out.println("Gugelcar(" + this.getName() + ") Terminando");
        bd.drawMap();
        super.finalize();
    }

    private void realizarLogin() {
        System.out.println("Enviando login.");
        realizarAccion(JSON.realizarLogin(this.MAPA));
        login = true;
        System.out.println("Pasamos a recibir datos login");
        estadoActual = RECIBIR_DATOS;
    }

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

    private void resultadoAccion() {
        System.out.println("Recibiendo respuesta.");
        boolean resultado = true;
        for (int i = 0; i < 4 && resultado; i++) {
            try {
                //System.out.println("Recibiendo respuesta2.");
                msjEntrada = receiveACLMessage();
                String recibido = msjEntrada.getContent();
                System.out.println("Respuesta recibida. " + recibido + "\n\n");
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
     * Heuristica de movimiento.
     *
     * @author Adrian Portillo Sanchez
     */
    private void updateMap() {
        mapa = bd.updateStatus(radar, gps, contadorPasos);
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
            return;
        } else if (posiblesObjetivos[row][col] == -1 || posiblesObjetivos[row][col] == 1) {
            return;
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
     * Determina el objetivo local dentro del 5x5 que rodea al gugelcar.
     *
     * @author Raúl López Arévalo private int[] chooseLocalObj() { int[]
     * objetive = new int[2]; float low_dist = (float) Math.pow(10, 10); int
     * low_moving_count = (int) Math.pow(10, 10); for (int i = 0; i <= 4; i++) {
     * for (int j = 0; j <= 4; j++) { if (posiblesObjetivos[i][j] == 0 && // Si
     * la casilla es accesible y: (mapa[datosGPS[1]-2+i][datosGPS[0]-2+j] <
     * low_moving_count || // o la casilla es menor // o la casilla es igual
     * pero más cercana al objetivo mapa[datosGPS[1]-2+i][datosGPS[0]-2+j] ==
     * low_moving_count && datosScanner[i][j] < low_dist)) { low_moving_count =
     * mapa[datosGPS[1]+i][datosGPS[0]+j]; low_dist = datosScanner[i][j];
     * objetive[0] = i; objetive[1] = j; } } } System.out.println( "Objetivo
     * final para moverse: " + objetive[0] + objetive[1]); return objetive; }
     */
    private int[] chooseLocalObj() {
        int[] objetive = new int[2];
        Arrays.deepToString(posiblesObjetivos);
        if (contadorPasos <= 2) { //cambiar a 1
            float low_dist = (float) Math.pow(10, 10);

            for (int i = 1; i <= 3; i++) {
                for (int j = 1; j <= 3; j++) {
                    System.out.println("Posible objetivo: " + posiblesObjetivos[i][j]);
                    System.out.println("Distancia menor: " + low_dist + "  datosScanner: " + datosScanner[i][j]);
                    if (posiblesObjetivos[i][j] == 0 && datosScanner[i][j] < low_dist) {
                        low_dist = datosScanner[i][j];
                        objetive[0] = i;
                        objetive[1] = j;
                        System.out.println("Encontrado nuevo objetivo mejor para moverse");
                    }
                }
            }
        }
        System.out.println("DATOS GPS :  " + datosGPS[0] + "," + datosGPS[1]);

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                System.out.print(posiblesObjetivos[i][j] + "-");
            }
            System.out.println("\n");
        }
        System.out.println("");

        if (contadorPasos > 2) {

            float low_dist2 = (float) Math.pow(10, 10);
            int low_moving_count = (int) Math.pow(10, 10);
            System.out.println("\t\t\tExplorando el mapa");
            for (int i = 0; i <= 4; i++) {
                for (int j = 0; j <= 4; j++) {
                    int a = datosGPS[0] - 2 + i;
                    int b = datosGPS[1] - 2 + j;
                    System.out.println("\t\t\tDatos del gps: " + datosGPS[0] + "," + datosGPS[1]);
                    System.out.println("\t\t\tVamos a acceder a: " + a + "," + b);
                    System.out.println("\t\t\tPosibles accesibles: " + Arrays.deepToString(posiblesObjetivos));
                    if (a >= 0 && b >= 0 && a<mapa.length && b<mapa.length) {
                        System.out.println("Entra primero");
                        if (posiblesObjetivos[i][j] == 0) {
                            if (mapa[a][b] < low_moving_count || (mapa[a][b] == low_moving_count && datosScanner[i][j] < low_dist2)) {
                                //if (posiblesObjetivos[i][j] == 0) {
                                System.out.println("Entra despues");
                                low_moving_count = mapa[a][b];
                                System.out.println(low_moving_count);
                                low_dist2 = datosScanner[i][j];
                                objetive[0] = i;
                                objetive[1] = j;
                                System.out.println("NUEVO OBJETIVO EXPERIMENTAL ENCONTRADO ----> " + objetive[0] + "," + objetive[1]);
                            }
                        }
                    }
                }

            }
            System.out.println(
                    "\n\n\t\tObjetivo experimental para moverse: " + objetive[0] + objetive[1]);
        }
        return objetive;
    }

    /**
     * Decide el mejor camino hacia un objetivo dado dentro del 5x5 que rodea al
     * gugelcar y devuelve el primer movimiento de ese camino.
     *
     * @author Adrian Portillo Sanchez
     */
    private String pathLocalObj(int objetivo) {
        String mov;
        //System.out.println("Objetivo candidato a moverse: " + objetivo[0]+ objetivo[1]);
        if (objetivo == 5) {
            mov = "moveN";
        } else if (objetivo == 4) {
            mov = "moveNE";
        } else if (objetivo == -1) {
            mov = "moveE";
        } else if (objetivo == -6) {
            mov = "moveSE";
        } else if (objetivo == -5) {
            mov = "moveS";
        } else if (objetivo == -4) {
            mov = "moveSW";
        } else if (objetivo == 1) {
            mov = "moveW";
        } else {
            mov = "moveNW";
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

    private void mover() {
        // Control sobre la batería
        if (nivelBateria < 10) {
            repostar();
        }
        // Limitar número de pasos
        if (contadorPasos == LIMITE_PASOS) {
            estadoActual = FINAL;
        } else {
            camino_aun = true;
            if (camino_aun) {
            posiblesObjetivos = new int[5][5];
            eliminarObjetivosInaccesibles();
                System.out.println("Posibles Objetivos: " + Arrays.deepToString(posiblesObjetivos));

            // Se decide qué casilla es mejor
            int[] objetivo_alcanzar = chooseLocalObj();
            int objetivo_id = objetivo_alcanzar[0] * 5 + objetivo_alcanzar[1];
            // Se calcula el camino optimo para llegar hasta ella

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

            camino.changeMap(posiblesObjetivos);
            camino.changeObjetive(objetivo_id);
                System.out.println("CAMBIOS REALIZADOS, OBJETIVO Y MATRIZ");
            camino.pathObjetive();
                System.out.println("NUEVO PATH OBTENIDO");
            path_local = camino.getPath();
                System.out.println("COORDENADAS DE TODO EL PATH --------------> ");
                for (int i = 0; i < path_local.size(); i++) {
                    System.out.print(path_local.get(i) / 5 + "-" + path_local.get(i) % 5 + " ");
                }
                System.out.println("\nCaminito pintao");
                camino.printPath();
                
                // transformo las id a coordenadas y se restan, se toman en valor abosulo y depende del resultado se sabe la dirección
                
                int primera_casilla = path_local.get(0);
                int segunda_casilla = path_local.get(1);
                
            int obj_prox_mov = primera_casilla - segunda_casilla;
            
                System.out.println("DIFERENCIA ENTRE CASILLAS --------------> " + obj_prox_mov);
                
            int [] obj_borrar = {path_local.get(1) / 5 , path_local.get(1) % 5};
                
                
            //int[] objetivo2 = {path_local.get(1) / 5, path_local.get(1) % 5};
                System.out.println("COORDENADAS A MOVERSE POR EL PRIMER PATH -------------------->  " + path_local.get(1) / 5 + "," + path_local.get(1) % 5);
                
              //  System.out.println("COORDENADAS A MOVERSE POR EL PRIMER PATH: " + obj_prox_mov[0] + obj_prox_mov[1]);
                // Se van haciendo cada uno de los movimientos
            String movimiento = pathLocalObj(obj_prox_mov);
            path_local.clear();
            updateMap();
                System.out.println("Datos del GPS bien puestos: " + datosGPS[0] + datosGPS[1] + "\n\t\tPaso numero: " + this.contadorPasos + "\n");
                contadorPasos++;
                nivelBateria--;
            realizarAccion(JSON.realizarAccion(movimiento));
                estadoActual = RECIBIR_DATOS;
                
            }
        }
        bd.drawMap();
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
