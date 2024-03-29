package gugelcar;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase encargada del conocimiento compartido de los agentes
 * 
 * @author Samuel Peregrina
 * @version 2.0
 */
public class Knowledge {
    private static Knowledge instance = null;
    private int map_id;
    private int[][] mapMatrix;
    private int[][] mapMatrixOptim;
    private int[] pos_actual = new int[2];
    private final int TAM_VISION = 5;
    private final int MIN_SIDE = 200;
    private int actual_max_size = MIN_SIDE;
        
    public final int STATE_FREE = 0;
    public final int STATE_WALL = -1;
    public final int STATE_GOAL = -2;
    public final int STATE_UNKNOW = 0;
    
    /**
     * Método que devuelve la instancia de Knowledge del agente
     * 
     * @param map_id El identificador del número de mapa que vamos a usar
     * @return La instancia de la BD
     */
    public static Knowledge getDB(int map_id){        
        if(instance == null){
            instance = new Knowledge(map_id);
        }else{ instance.setMapID(map_id); }
        return instance;
    }
    
    /********** Estos métodos han sido añadidos por Raúl López Arévalo*********
     * @autor Raúl López Arévalo
     * 
     * Comprueba si un valor está contenido en la matriz o no
     * @param value Valor a comprobar
     * @return true si está
     */
    public boolean contains( int value ){
        boolean exist_value = false;
        for( int i = 0; i < this.mapMatrix.length; i++){
            for ( int j = 0; j < this.mapMatrix.length; j++){
                if( this.mapMatrix[i][j] == value ){
                    exist_value = true;
                }
            }
        }
        return exist_value;
    }    
    
    /**
     * @autor Raúl López Arévalo
     * 
     * Devuelve el ID que le corresponde a a un valor de la matriz si está
     * 
     * @param value Valor del que queremos obtener su ID
     * @return id ID del valor
     */
    public int getIDValue( int value ){
        int id = -1;
        for( int i = 0; i < this.mapMatrix.length; i++){
            for ( int j = 0; j < this.mapMatrix.length; j++){
                if( this.mapMatrix[i][j] == value ){
                    id = i*this.mapMatrix.length+j;
                }
            }
        }
        return id;
    }
    
    
    /**
     * @autor Raúl López Arévalo
     * 
     * Devuelve el tamaño del mapa
     * 
     * @return Tamañao del mapa 
     */
    public int tamMap(){
        return this.mapMatrix.length;
    }
    
    /**
     * @autor Raúl López Arévalo
     * 
     * Transforma el conocimiento del mapa en otro mapa. Las posiciones no 
     * visitadas se toman como muros.
     * @return 
     */
    public int[][] getMap(){
        this.mapMatrixOptim = new int[this.mapMatrix.length][this.mapMatrix.length];
        for( int i = 0; i < this.mapMatrixOptim.length; i++){
            for ( int j = 0; j < this.mapMatrixOptim.length; j++){
                mapMatrixOptim[i][j] = mapMatrix[i][j];
                if( this.mapMatrixOptim[i][j] == 0 ){
                    this.mapMatrixOptim[i][j] = -1;
                }
                if( this.mapMatrixOptim[i][j] >0){
                    this.mapMatrixOptim[i][j] = 0;
                }
            }
        }
        return this.mapMatrixOptim;
    }
    
    
    public void printoptim(){
        for(int i = 0; i < this.mapMatrixOptim.length; i++){
            for (int j = 0; j < this.mapMatrixOptim[i].length; j++) {
                int value = this.mapMatrixOptim[i][j];
                if(j == 0) System.out.print("▉▉▉");
                //if(pos_actual[0] == i && pos_actual[1] == j) System.out.print(" ⎔ ");
                if(pos_actual[0] == i && pos_actual[1] == j) System.out.print(" ● ");
                else{
                    switch (value) {
                        case 0:
                            System.out.print(" ⎕ ");
                            break;
                        case -1:
                            System.out.print("▉▉▉");
                            break;
                        case -2:
                            System.out.print(" ╳ ");
                            break;
                        default:
                            if(value < 10) System.out.print(" " + value+ " ");
                            else if(value < 100) System.out.print(" " + value);
                            else System.out.print(value);
                            break;
                    }
                }
            }
            System.out.print("\n");
    }
    }
    /**
     * 
     *              FIN MÉTODOS RAÚL LÓPEZ
     */
    
    /**
     * @author Samuel Peregrina
     * @param id 
     */
    private void setMapID(int id){
        this.map_id = id;
    }
    /**
     * @author Samuel Peregrina
     * Inicializa la matriz de conocimiento
     * @param map_id ID de la matriz correspondiente al numero de mapa
     */
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
     * @author Samuel Peregrina
     *  Este método es el encargado de recibir los datos obtenidos por el agente y 
     *  añadirlos a su conocimiento
     * 
     * @param radar JsonObject que contiene la información del radar
     * @param gps JsonObject que contiene la posición del agente
     * @param turn int que contiene el número discreto del turno del movimiento del agente
     */
    public void updateStatus(JsonObject radar, JsonObject gps, int turn) {
        Connection connection  = null;
        try{
            int position_x, position_y;
            // Guardamos la posición actual del agente
            JsonObject gpsObject = gps.get("gps").asObject();
            position_x = gpsObject.get("y").asInt();
            position_y = gpsObject.get("x").asInt();
            
            this.pos_actual[0] = position_x;
            this.pos_actual[1] = position_y;
            
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
            
            // Limites de las columnas
            if(radarMatrix.get(2*this.TAM_VISION + 0) == 1) lim_sup_col = 1;
            if(radarMatrix.get(2*this.TAM_VISION + 1) == 1) lim_sup_col = 2;
            if(radarMatrix.get(2*this.TAM_VISION + 4) == 1) lim_inf_col = 3;
            if(radarMatrix.get(2*this.TAM_VISION + 3) == 1) lim_inf_col = 2;
            
            // Limites de las filas
            if(radarMatrix.get(0*this.TAM_VISION + 2) == 1) lim_sup_row = 1;
            if(radarMatrix.get(1*this.TAM_VISION + 2) == 1) lim_sup_row = 2;
            if(radarMatrix.get(4*this.TAM_VISION + 2) == 1) lim_inf_row = 3;
            if(radarMatrix.get(3*this.TAM_VISION + 2) == 1) lim_inf_row = 2;
            
            this.actual_max_size = Math.max(this.actual_max_size, Math.max(position_x + (this.TAM_VISION/2), position_y + (this.TAM_VISION/2)));

            // Nos conectamos a la DB
            connection = DriverManager.getConnection("jdbc:sqlite:mapas.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);

            for (int i = 0; i < this.TAM_VISION; i++) {                
                for (int j = 0; j < this.TAM_VISION; j++) {
                    int pos_x = (position_x -(this.TAM_VISION/2) + i);
                    int pos_y = (position_y -(this.TAM_VISION/2) + j);
                    int radarValue = radarMatrix.get(i*this.TAM_VISION + j);
                    int state = (radarValue == STATE_FREE) ? turn : radarValue*(-1);
                    
                    if((radarValue == 1 || (j >= lim_sup_col && j <= lim_inf_col && i >= lim_sup_row && i <= lim_inf_row)) && pos_x >= 0 && pos_y >= 0){
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
    }
    
    /**
     * @author Samuel Peregrina
     * Este método actualiza un valor de estado de una coordenada.
     * En caso necesario redimensiona la matriz.
     * 
     * @param posx int Posición x de la coordenada
     * @param posy int Posición y de la coordenada
     * @param value int Valor a actualizar
     */
    private void updateMatrix(int posx, int posy, int value){                
        // Comprobamos si hay que redimensionar la matriz
                
        // Nos aseguramos que el tamaño de la matriz sea como mínimo 5 casillas más que la posición del agente
        int diff_min = this.tamMap() - Math.max(this.pos_actual[0], this.pos_actual[1]);
        if(diff_min < 5){
            int diff = this.tamMap() + (5 - diff_min);
            
            int[][] tmp = this.mapMatrix;
            this.mapMatrix = new int[diff][diff];
            for(int i = 0; i < tmp.length; i++){
                for(int j = 0; j < tmp[i].length; j++){
                    this.mapMatrix[i][j] = tmp[i][j];
                }
            } 
        }
        
        this.mapMatrix[posx][posy] = value;
    }
    
    /**
     * @author Samuel Peregrina
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
            
            String sqlCount;
            ResultSet rs;
            
            // Comprobamos si ya hay información del mapa
            sqlCount = "SELECT COUNT(*) AS count FROM Mapa_" + this.map_id + ";";
            
            rs = statement.executeQuery(sqlCount);
            while(rs.next()){
                matrix_size = rs.getInt("count");
            }
            
            System.out.println("\nCantidad de celdas conocidas: " + matrix_size);

            if(matrix_size > 0) {
                matrix_size = 0;
                // Calculamos el tamaño máximo de la matriz
                // Por posición X
                sqlCount = "SELECT MAX(pos_x) AS count FROM Mapa_" + this.map_id + ";";

                rs = statement.executeQuery(sqlCount);
                while(rs.next()){
                    matrix_size = Math.max(matrix_size, (rs.getInt("count") + 1));
                }

                // Por posición Y
                sqlCount = "SELECT MAX(pos_y) AS count FROM Mapa_" + this.map_id + ";";

                rs = statement.executeQuery(sqlCount);
                while(rs.next()){
                    matrix_size = Math.max(matrix_size, (rs.getInt("count") + 1));
                }     

                System.out.println("El máximo de la matriz es: " + matrix_size);
            
            
                // Creamos la matriz con el tamaño conocido
                this.mapMatrix = new int[matrix_size][matrix_size];

                // Obtenemos la información almacenada y la volcamos en la matriz
                rs = statement.executeQuery("SELECT * FROM Mapa_"+this.map_id +";");
                while(rs.next()){
                    this.mapMatrix[rs.getInt("pos_x")][rs.getInt("pos_y")] = rs.getInt("state");
                }
            }else{
                System.out.println("Creamos matriz desde cero");
                
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
    
    /**
     * @author Samuel Peregrina
     * Este método dibuja el mapa conocido por el agente
     */
    public void drawMap(){
        System.out.println("| Mapa actual - Filas: " + this.mapMatrix.length + " | Columnas: " + this.mapMatrix[0].length);
        for(int i = 0; i < actual_max_size;i++) System.out.print("▉▉▉");
        System.out.println("");
        for(int i = 0; i < this.mapMatrix.length; i++){
            for (int j = 0; j < this.mapMatrix[i].length; j++) {
                int value = this.mapMatrix[i][j];
                if(j == 0) System.out.print("▉▉▉");
                //if(pos_actual[0] == i && pos_actual[1] == j) System.out.print(" ⎔ ");
                if(pos_actual[0] == i && pos_actual[1] == j) System.out.print(" ● ");
                else{
                    switch (value) {
                        case 0:
                            System.out.print(" ⎕ ");
                            break;
                        case -1:
                            System.out.print("▉▉▉");
                            break;
                        case -2:
                            System.out.print(" ╳ ");
                            break;
                        default:
                            if(value < 10) System.out.print(" " + value+ " ");
                            else if(value < 100) System.out.print(" " + value);
                            else System.out.print(value);
                            break;
                    }
                }
            }
            System.out.print("\n");
        }
        
        System.out.println("/////////////////////////////////////////////////////////////////////////////////////////////////////");
    }
    
    /**
     * @author Samuel Peregrina
     * Devuelve el contenido en el mapa conocido del agente.
     * En caso de que la posición pedida esté fuera del conocimiento devuelve STATE_WALL
     * 
     * @param px Posición X a obtener
     * @param py Posición Y a obtener
     * @return int Que contiene el contenido en las coordenadas pedidas
     */
    public int getStatus(int px, int py){
        return (px < 0 || py < 0 || px > this.actual_max_size  || py > this.actual_max_size) ? this.STATE_WALL : this.mapMatrix[px][py];
    }
    
    /**
     * @author Samuel Peregrina
     * Devuelve el tamaño máximo conocido del mapa actual
     * 
     * @return El tamaño máximo conocido de la matriz
     */
    public int getMatrixSize(){
        return this.actual_max_size;
    }
}