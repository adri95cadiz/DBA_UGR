package gugelcar;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Clase encargada del conocimiento compartido de los agentes
 * 
 * @author Samuel Peregrina
 * @version 1.0
 */
public class Knowledge {
    private static Knowledge instance = null;
    private int map_id;
    private int[][] mapMatrix;
    private final int TAM_VISION = 5;
    private final int MIN_SIDE = 20;
    private int actual_max_size = MIN_SIDE;
        
    public final int STATE_FREE = 0;
    public final int STATE_WALL = -1;
    public final int STATE_GOAL = -2;
    public final int STATE_UNKNOW = 0;
    
    /**
     * Método que devuelve la instancia de Knowledge del agente
     * 
     * @param map_id El identificador del número de mapa que vamos a usar
     * @return
     */
    public static Knowledge getDB(int map_id){        
        if(instance == null){
            instance = new Knowledge(map_id);
        }else{ instance.setMapID(map_id); };
        return instance;
    }
    
    private void setMapID(int id){
        this.map_id = id;
    }

    private Knowledge(int map_id) {     
        Connection connection = null;
        this.setMapID(map_id);
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:mapas.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
            
            String creteTableSQL = "CREATE TABLE IF NOT EXISTS Mapa_" + this.map_id + " ("+
                "pos_x INTEGER NOT NULL,"+
                "pos_y INTEGER NOT NULL,"+
                "radar INTEGER DEFAULT 0,"+
                "state INTEGER DEFAULT "+ STATE_UNKNOW + ","+
                "CONSTRAINT pk_posicion PRIMARY KEY (pos_x, pos_y)"+
            ")";

            int state = statement.executeUpdate(creteTableSQL);
            this.createMatrix();
        } catch (SQLException e) {
            System.err.println("Error en la creación de la DB");
            System.err.println(e);
        } finally {
            try {
                if(connection != null) connection.close();
            } catch(SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }
    
    /**
     *  Este método es el encargado de recibir los datos obtenidos por el agente y 
     *  añadirlos a su conocimiento
     * 
     * @param radar JsonObject que contiene la información del radar
     * @param gps JsonObject que contiene la posición del agente
     * @param turn int que contiene el número discreto del turno del movimiento del agente
     * @return Una matriz que contiene todo el conociento del agente sobre el mapa
     */
    public int[][] updateStatus(JsonObject radar, JsonObject gps, int turn) {
        Connection connection  = null;
        try{
            int position_x, position_y;
            // Guardamos la posición actual del agente
            JsonObject gpsObject = gps.get("gps").asObject();
            position_x = gpsObject.get("x").asInt();
            position_y = gpsObject.get("y").asInt();
            
            //Transformamos el array JSON del radar a un array de int
            JsonArray radarJson = radar.get("radar").asArray();
            ArrayList<Integer> radarMatrix = new ArrayList<>();
            
            for (int i = 0; i < radarJson.size(); i++) {                
                radarMatrix.add(radarJson.get(i).asInt());
            }
            
            // Creamos un algoritmo para calcular que filas debemos de rellenar
            int lim_sup_col = 0;
            int lim_inf_col = this.TAM_VISION;
            int lim_sup_row = 0;
            int lim_inf_row = this.TAM_VISION;
            
            if(radarMatrix.get(2*this.TAM_VISION + 0) == 1) lim_sup_col = 1;
            if(radarMatrix.get(2*this.TAM_VISION + 1) == 1) lim_sup_col = 2;
            if(radarMatrix.get(2*this.TAM_VISION + 4) == 1) lim_inf_col = 3;
            if(radarMatrix.get(2*this.TAM_VISION + 3) == 1) lim_inf_col = 2;

            if(radarMatrix.get(0*this.TAM_VISION + 2) == 1) lim_sup_row = 1;
            if(radarMatrix.get(1*this.TAM_VISION + 2) == 1) lim_sup_row = 2;
            if(radarMatrix.get(4*this.TAM_VISION + 2) == 1) lim_inf_row = 3;
            if(radarMatrix.get(3*this.TAM_VISION + 2) == 1) lim_inf_row = 2;
            
            /*ArrayList<Integer> lista_filas = new ArrayList<>();
            for(int i = 0; i < this.TAM_VISION; i++){
                for(int j = 0; j < this.TAM_VISION; j++){
                   int valor_pos = radarMatrix.get(i*this.TAM_VISION + j);
                   //System.out.print(valor_pos +",");
                   if( valor_pos == 1){
                       // Limites por filas
                       if(i < this.TAM_VISION/2){
                           lim_sup_row = Math.max(lim_sup_row, i);
                       } else if(i > this.TAM_VISION/2) {
                           lim_inf_row = Math.min(lim_inf_row, i);
                       }
                       // Limites por columnas
                       if(j < this.TAM_VISION/2){
                           lim_sup_col = Math.max(lim_sup_col, j);
                       } else if(j > this.TAM_VISION/2) {
                           lim_inf_col = Math.min(lim_inf_col, j);
                       }
                   }
                }
            }*/
            
            this.actual_max_size = Math.max(this.actual_max_size, Math.max(position_x + (this.TAM_VISION/2), position_y + (this.TAM_VISION/2)));

            // Nos conectamos a la DB
            connection = DriverManager.getConnection("jdbc:sqlite:mapas.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);

            for (int i = lim_inf_row; i < lim_sup_row; i++) {                
                for (int j = 0; j < this.TAM_VISION; j++) {
                    int pos_x = (position_x -(this.TAM_VISION/2) + i);
                    int pos_y = (position_y -(this.TAM_VISION/2) + j);
                    int radarValue = radarMatrix.get(i*this.TAM_VISION + j);
                    int state = (radarValue == STATE_FREE) ? turn : radarValue*(-1);
                    
                    if(radarValue == 1 || (j >= lim_sup_col && j <= lim_inf_col)){
                        String querySQL = "INSERT OR REPLACE INTO Mapa_"+this.map_id+"(pos_x, pos_y, radar, state) VALUES("
                                + pos_x + ", " 
                                + pos_y + ", "
                                + radarValue + ", "
                                + state
                            +");";
                        //Ejecutamos la consulta
                        statement.executeUpdate(querySQL);

                        //Actualizamos la fila y de la matriz
                        updateMatrix(pos_x, pos_y, state);
                    }
                }
            }
            /*for (int j = lim_inf_col; j < lim_sup_col; j++) {                
                for (int i = 0; i < this.TAM_VISION; i++) {
                    int pos_x = (position_x -(this.TAM_VISION/2) + i);
                    int pos_y = (position_y -(this.TAM_VISION/2) + j);
                    int radarValue = radarMatrix.get(i*this.TAM_VISION + j);
                    int state = (radarValue == STATE_FREE) ? turn : radarValue*(-1);
                    
                    if(radarValue == 1 || (i >= lim_sup_row && i <= lim_inf_row)){
                        String querySQL = "INSERT OR REPLACE INTO Mapa_"+this.map_id+"(pos_x, pos_y, radar, state) VALUES("
                                + pos_x + ", " 
                                + pos_y + ", "
                                + radarValue + ", "
                                + state
                            +");";
                        //Ejecutamos la consulta
                        statement.executeUpdate(querySQL);

                        //Actualizamos la fila y de la matriz
                        updateMatrix(pos_x, pos_y, state);
                    }
                }
            }*/
        } catch(SQLException e){
            System.err.println("Error en la actualización");
            System.err.println(e);
        } finally {
            try {
                if(connection != null) connection.close();
            } catch(SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
        
        return this.mapMatrix;
    }
    
    /**
     * Este método actualiza un valor de estado de una coordenada.
     * En caso necesario redimensiona la matriz.
     * 
     * @param posx int Posición x de la coordenada
     * @param posy int Posición y de la coordenada
     * @param value int Valor a actualizar
     */
    private void updateMatrix(int posx, int posy, int value){
        // Comprobamos el tamaño de la matriz
        int max = Math.max(this.mapMatrix.length, this.actual_max_size+1);
        
        // Si nuestra matriz es más pequeña, le aumentamos el tamaño
        if(max > this.mapMatrix.length){
            int[][] tmp = this.mapMatrix;
            this.mapMatrix = new int[max][max];
            for(int i = 0; i < tmp.length; i++){
                for(int j = 0; j < tmp[i].length; j++){
                    this.mapMatrix[i][j] = tmp[i][j];
                }
            }
        }
        
        this.mapMatrix[posx][posy] = value;
    }
    
    /**
     * Este método se encarga de crear la matriz, si tiene datos carga los datos.
     * En el caso de que todavía no hubiese datos inicializa la matriz con valores por defecto.
     */
    private void createMatrix() {
        Connection connection = null;
        try {
            int matrix_size = 0;
            // Nos conectamos a la DB
            connection = DriverManager.getConnection("jdbc:sqlite:mapas.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);

            // Calculamos el tamaño del mapa que conocemos
            String sqlCount = "SELECT MAX(pos_x, pos_y) AS count FROM Mapa_" + this.map_id + ";";
            System.out.println("Executing: " + sqlCount);
            ResultSet rs = statement.executeQuery(sqlCount);
            while(rs.next()){
                matrix_size = rs.getInt("count") + 1;
            }
            System.out.println("El máximo de la matriz es: " + matrix_size);
            
            if(matrix_size > 0) {
                // Creamos la matriz con el tamaño conocido
                this.mapMatrix = new int[matrix_size][matrix_size];

                // Obtenemos la información almacenada y la volcamos en la matriz
                rs = statement.executeQuery("SELECT * FROM Mapa_"+this.map_id +";");
                while(rs.next()){
                    this.mapMatrix[rs.getInt("pos_x")][rs.getInt("pos_y")] = rs.getInt("state");
                }
            }else{
                this.mapMatrix = new int[MIN_SIDE][MIN_SIDE];
            }
        } catch(SQLException e){
            System.err.println(e);
        } finally {
            try {
                if(connection != null) connection.close();
            } catch(SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }
}