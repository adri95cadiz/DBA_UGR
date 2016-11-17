package gugelcar;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Clase encargada del conocimiento compartido de los agentes
 * 
 * @author Samuel Peregrina
 * @version 1.0
 */
public class Knowledge {
    private Knowledge instance = null;
    private int map_id;
    private int[][] mapMatrix;
    private final int TAM_VISION = 5;
    private final int MIN_SIDE = 20;
        
    public final int STATE_FREE = 0;
    public final int STATE_WALL = -1;
    public final int STATE_GOAL = -2;
    public final int STATE_UNKNOW = -5;
    
    /**
     * Método que devuelve la instancia de Knowledge del agente
     * 
     * @param map_id El identificador del número de mapa que vamos a usar
     * @return
     */
    public Knowledge getDB(int map_id){        
        if(instance == null){
            instance = new Knowledge();
        }
        this.map_id = map_id;
        return instance;
    }

    private Knowledge() {     
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:mapas.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
            
            String creteTableSQL = "CREATE TABLE IF NOT EXIST Mapa_" + this.map_id + "("+
                "pos_x INTEGER NOT NULL,"+
                "pos_y INTEGER NOT NULL,"+
                "radar INTEGER DEFAULT 0,"+
                "state INTEGER DEFAULT "+ STATE_UNKNOW + ","+
                "CONSTRAINT pk_posicion PRIMARY KEY (pos_x, pos_y)"+
            ")";

            statement.executeUpdate(creteTableSQL);
            this.createMatrix();
        } catch (SQLException e) {
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
            position_x = gps.get("x").asInt();
            position_y = gps.get("y").asInt();
            
            //Transformamos el array JSON del radar a un array de int
            JsonArray radarJson = radar.get("radar").asArray();
            int[] radarMatrix = new int[radarJson.size()];   
            
            for (int i = 0; i < radarJson.size(); i++) {
                radarMatrix[i] = radarJson.get(i).asInt();
            }
            
            // Nos conectamos a la DB
            connection = DriverManager.getConnection("jdbc:sqlite:mapas.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);

            for (int i = 0; i < this.TAM_VISION; i++) {
                
                for (int j = 0; j < this.TAM_VISION; j++) {
                    int pos_x = (position_x -(this.TAM_VISION/2) + i);
                    int pos_y = (position_y -(this.TAM_VISION/2) + j);
                    int radarValue = radarMatrix[i*this.TAM_VISION + j];
                    int state = radarValue == STATE_FREE ? turn : radarValue*(-1);
                    
                    String querySQL = "INSERT OR UPDATE INTO Map_"+this.map_id+"(pos_x, pos_y, radar, state) VALUES("
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
        int max = Math.max(this.mapMatrix.length, Math.max(posx,posy)) + 1;
        
        // Si nuestra matriz es más pequeña, le aumentamos el tamaño
        if(max > this.mapMatrix.length){
            int[][] tmp = this.mapMatrix;
            this.mapMatrix = new int[max][max];
            for (int i = tmp.length; i < max; i++) {
                Arrays.fill(this.mapMatrix[i], STATE_UNKNOW);
            }
            System.arraycopy(tmp, 0, this.mapMatrix, 0, tmp.length);
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
            String sqlCount = "SELECT COUNT(*) AS count FROM Map_" + this.map_id + ";";
            ResultSet rs = statement.executeQuery(sqlCount);
            while(rs.next()){
                matrix_size = rs.getInt("count");
            }
            
            if(matrix_size > 0) {
                // Creamos la matriz con el tamaño conocido
                this.mapMatrix = new int[matrix_size][matrix_size];

                // Obtenemos la información almacenada y la volcamos en la matriz
                rs = statement.executeQuery("SELECT * FROM Map_"+this.map_id +";");
                while(rs.next()){
                    this.mapMatrix[rs.getInt("pos_x")][rs.getInt("pos_y")] = rs.getInt("value");
                }
            }else{
                this.mapMatrix = new int[MIN_SIDE][MIN_SIDE];
                for(int[] row : this.mapMatrix){
                    Arrays.fill(row, STATE_UNKNOW);
                }
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