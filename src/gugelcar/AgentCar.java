package gugelcar;

import edu.emory.mathcs.backport.java.util.Arrays;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

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
    private int nivelBateria=0;
    private int[] datosGPS = new int[2];
    private int[][] datosRadar = new int[5][5];
    private float[][] datosScanner = new float[5][5];
    private int[][] posiblesObjetivos = new int[5][5];
    private int cont;
    
  public AgentCar(AgentID aid) throws Exception {
    super(aid);
  }

  /**
   * Inicializamos variables
   * @author Luis Gallego
   */
  @Override
  public void init() {
      System.out.println("Iniciando GugelCar.");
      estadoActual = LOGIN;
      msjSalida = null;
      msjEntrada = null;
      fin = false;
      cont = 0;
  }

  /**
   * Metodo que ejecuta el agente donde controlamos los estados
   * @author Luis Gallego
   */
  @Override
  public void execute() {
     System.out.println("Agente activo.");     
     while(!fin) {
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
    System.out.println("Gugelcar("+ this.getName() +") Terminando");
    super.finalize();
  }
  
  private void realizarLogin() {
      System.out.println("Enviando login.");
      realizarAccion(JSON.realizarLogin());
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
      for(int i=0; i<4 && resultado; i++) {
          try {
              //System.out.println("Recibiendo respuesta2.");
              msjEntrada = receiveACLMessage();
              System.out.println("Respuesta recibida. " + msjEntrada.getContent());     
             if(msjEntrada.getContent().contains("scanner")) {
                  datosScanner = JSON.leerScanner(msjEntrada.getContent());
              } else if(msjEntrada.getContent().contains("radar")) {
                  datosRadar = JSON.leerRadar(msjEntrada.getContent());
              } else if(msjEntrada.getContent().contains("gps")) {
                  datosGPS = JSON.leerGPS(msjEntrada.getContent());
              } else if(login) {
                  System.out.println("ResultadoLogin: ");
                  resultado = JSON.resultadoLogin(msjEntrada.getContent());
                  login = false;
              } else {
                  resultado = JSON.resultadoAccion(msjEntrada.getContent());
              }                 
              
          } catch (InterruptedException ex) {
              System.err.println("Error de comunicación");
          }
      }   
      
      if(!resultado) {
          System.out.println("La conexión ha fallado.");
          estadoActual = FINAL;
      } else if(datosRadar[2][2]==2) {
          estadoActual = FINAL;
      } else if(nivelBateria == 1) {
          estadoActual = REPOSTAR;
      } else {
          estadoActual = ACCION;
      }
  }
  
  /**
   * Heuristica de movimiento.
   * @author Adrian Portillo Sanchez
   */
  private void mover() {
      /*if(cont==100){
          estadoActual = FINAL;
      } else {
          cont++;
          nivelBateria--;
          realizarAccion(JSON.realizarAccion("moveSW"));
          estadoActual = RECIBIR_DATOS;
      }*/
	  posiblesObjetivos = new int[5][5];
	  eliminarObjetivosInaccesibles();
	  System.out.println("Posibles Objetivos: " + Arrays.deepToString(posiblesObjetivos));
	  int[] objetivo = elegirObjetivo();
	  String movimiento = caminoActual(objetivo);
	  cont++;
	  nivelBateria--;
      realizarAccion(JSON.realizarAccion(movimiento));
      estadoActual = RECIBIR_DATOS;
  }
  
  /**
   * Pone los objetivos inaccesibles del 5x5 que rodea al gugelcar a -1 y los accesibles a 0.
   * @author Adrian Portillo Sanchez
   */
  private void eliminarObjetivosInaccesibles( ) {
	  eliminarObjetivosInaccesiblesRec(2,2);
	  posiblesObjetivos[2][2] = -1;		//Pone la posicion actual a -1, no se deberia deliberar sobre ella.
	  for(int i = 0; i < 5; i++) {		//Pone los objetivos no alcanzados a -1, tampoco son accesibles.
		  for(int j = 0; j < 5; j++) {
			  if(posiblesObjetivos[i][j] == 0) posiblesObjetivos[i][j] = -1;
		  }
	  }
	  for(int i = 0; i < 5; i++) {		//Pone los 1's a 0's dejando finalmente los accesibles a 0 y los inaccesibles a -1.
		  for(int j = 0; j < 5; j++) {
			  if(posiblesObjetivos[i][j] == 1) posiblesObjetivos[i][j] = 0;
		  }
	  }
  }
  
  /**
   * Funcion recursiva .
   * @author Adrian Portillo Sanchez
   */
  private void eliminarObjetivosInaccesiblesRec(int row, int col) {
	  if (row < 0 || row > 4 || col < 0 || col > 4) return;
	  else if(posiblesObjetivos[row][col] == -1 || posiblesObjetivos[row][col] == 1) return;
	  else if(datosRadar[row][col] == 1) posiblesObjetivos[row][col] = -1;
	  else{
		  posiblesObjetivos[row][col] = 1;
		  eliminarObjetivosInaccesiblesRec(row-1,col-1);	//Superior izquierdo.
		  eliminarObjetivosInaccesiblesRec(row-1,col);		//Superior centro.
		  eliminarObjetivosInaccesiblesRec(row-1,col+1);	//Superior derecho.
		  eliminarObjetivosInaccesiblesRec(row,col-1);		//Centro izquierdo.
		  eliminarObjetivosInaccesiblesRec(row,col+1);		//Centro derecho.
		  eliminarObjetivosInaccesiblesRec(row+1,col-1);	//Inferior izquierdo.
		  eliminarObjetivosInaccesiblesRec(row+1,col);		//Inferior centro.
		  eliminarObjetivosInaccesiblesRec(row+1,col+1);	//Inferior derecho.	
	  }
  }
  
  /**
   * Determina el objetivo local dentro del 5x5 que rodea al gugelcar.
   * @author Adrian Portillo Sanchez
   */
  private int[] elegirObjetivo() {
	  int[] objetivo = new int[2];
	  return objetivo;
  }
  
  /**
   * Decide el mejor camino hacia un objetivo dado dentro del 5x5 que rodea al gugelcar y devuelve el primer movimiento de ese camino.
   * @author Adrian Portillo Sanchez
   */
  private String caminoActual(int[] objetivo) {
	  return("moveSW");
  }
  
  /**
   * Estado repostar.
   * @author Luis Gallego
   */
  private void repostar() {
      System.out.println("Respostando.");
      realizarAccion(JSON.realizarAccion("refuel"));
      nivelBateria = 100;
      estadoActual = RECIBIR_DATOS;
  }
  
  /**
   * Estado final.
   * @author Luis Gallego
   */
  private void objetivoEncontrado() {
      System.out.println("Objetivo encontrado.");
      realizarAccion(JSON.realizarAccion("logout"));
      resultadoAccion();
      System.out.println("Pasos: " + cont);
      fin = true;      
  }
  
}
