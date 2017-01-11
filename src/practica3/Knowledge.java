package practica3;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Clase encargada del conocimiento compartido de los agentes
 *
 * @author Samuel Peregrina
 * @version 3.0
 */
public class Knowledge {
    private static Knowledge instance = null;
    private int map_id;
    private int[][] mapMatrix;
    private Connection connection = null;
    private ArrayList<AgentPosition> agentsPosition = new ArrayList<AgentPosition>();

    private final int MIN_SIDE = 20;
    private final String DB_NAME = "knowledge";
    public final int STATE_FREE = 0;
    public final int STATE_WALL = 1;
    public final int STATE_WORLD_END = 2;
    public final int STATE_GOAL = 3;
    public final int STATE_VEHICLE = 4;
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

    /**
     * Método que devuelve un Statement para realizar una acción en la BDG
     * 
     * @return statement Un objeto Statement para realizar la consulta
     */
    private Statement getStatement(){
        Statement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:"+ DB_NAME +".db");
            statement = connection.createStatement();
            statement.setQueryTimeout(30);
        }catch(SQLException error){
            System.err.println("Error en la creación de la conexión");
            System.err.println(error);
        }finally{
            return statement;
        }
    }

    /**
     * Método para ejecutar una consulta en la BD
     * Este método debe de usarse cuando es una consulta única.
     * Nunca debe de usarse dentro de un bucle ya que una vez ejecuta la consulta 
     * cierra la conexión con la BD.
     *
     * @param query Consulta SQL a ejecutar
     */
    private void executeUpdate(String query){
        try{
            Statement st = getStatement();
            st.executeUpdate(query);
            this.connection.close();
        }catch(SQLException e){
            System.err.println("Error en la ejecución de la consulta");
            System.err.println(e);
        }
    }

    /**
     * Comprueba si un valor está contenido en la matriz
     * @param value Valor a comprobar
     * @return true si está
     * @autor Raúl López Arévalo
     */
    public boolean contains( int value ){
        boolean exist_value = false;
        for( int i = 0; i < this.mapMatrix.length && !exist_value; i++){
            for ( int j = 0; j < this.mapMatrix.length && !exist_value; j++){
                if( this.mapMatrix[i][j] == value ){
                    exist_value = true;
                }
            }
        }
        return exist_value;
    }

    /**
     * Devuelve el tamaño del mapa
     *
     * @return Tamañao del mapa
     * @autor Raúl López Arévalo
     */
    public int mapSize(){
        return this.mapMatrix.length;
    }

    /**
     * Asigna el identificador del mapa a usar
     *
     * @param id ID correspondiente al mapa a usar
     */
    private void setMapID(int id){
        this.map_id = id;
    }

    /**
     * Inicializa la matriz de conocimiento
     *
     * @param map_id ID de la matriz correspondiente al numero de mapa
     */
    private Knowledge(int map_id) {
        Connection connection = null;
        this.setMapID(map_id);
        try {
            String creteTableSQL = "CREATE TABLE IF NOT EXISTS Mapa_" + this.map_id + " ("+
                "pos_x INTEGER NOT NULL,"+
                "pos_y INTEGER NOT NULL,"+
                "contains INTEGER DEFAULT 0,"+
                "CONSTRAINT pk_posicion PRIMARY KEY (pos_x, pos_y)"+
            ")";

            this.executeUpdate(creteTableSQL);
            this.createMatrix();
        } catch (Exception e) {
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
     */
    public void updateStatus(String agentName, JsonObject radar, JsonObject gps) {
        try{
            int position_x, position_y;
            // Guardamos la posición actual del agente
            JsonObject gpsObject = gps.get("gps").asObject();
            position_x = gpsObject.get("y").asInt();
            position_y = gpsObject.get("x").asInt();

            this.setAgentPosition(agentName, position_x, position_y);

            //Transformamos el array JSON del radar a un array de int
            JsonArray radarJson = radar.get("radar").asArray();
            ArrayList<Integer> radarMatrix = new ArrayList<>();

            for (int i = 0; i < radarJson.size(); i++) {
                radarMatrix.add(radarJson.get(i).asInt());
            }
            
            // Calculamos el radio de visión a partir del radar
            int tamVision = (int)Math.sqrt(radarMatrix.size());

            // Nos conectamos a la DB
            Statement statement = this.getStatement();

            for (int i = 0; i < tamVision; i++) {
                for (int j = 0; j < tamVision; j++) {
                    int pos_x = (position_x -(tamVision/2) + i);
                    int pos_y = (position_y -(tamVision/2) + j);
                    int radarValue = radarMatrix.get(i*tamVision + j);

                    if(pos_x >= 0 && pos_y >= 0){
                        String querySQL = "INSERT OR REPLACE INTO Mapa_"+this.map_id+"(pos_x, pos_y, contains) VALUES("
                                + pos_x + ", "
                                + pos_y + ", "
                                + radarValue
                            +");";
                        //Ejecutamos la consulta
                        statement.executeUpdate(querySQL);

                        //Actualizamos la fila y de la matriz
                        updateMatrix(pos_x, pos_y, radarValue);
                    }
                }
            }
        } catch(SQLException e){
            System.err.println("Error en la actualización");
            System.err.println(e);
        } finally {
            try {
                if(connection != null) this.connection.close();
            } catch(SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
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
        int maxWidth = Math.max(this.mapSize(), Math.max(posx, posy));

        int[][] tmp = this.mapMatrix;
        this.mapMatrix = new int[maxWidth][maxWidth];
        for(int i = 0; i < tmp.length; i++){
            for(int j = 0; j < tmp[i].length; j++){
                this.mapMatrix[i][j] = tmp[i][j];
            }
        }

        this.mapMatrix[posx][posy] = value;
    }

    /**
     * Este método se encarga de crear la matriz, si tiene datos carga los datos.
     * En el caso de que todavía no hubiese datos inicializa la matriz con valores por defecto.
     */
    private void createMatrix() {
        try {
            int matrix_size = 0;
            // Nos conectamos a la DB
            Statement statement = this.getStatement();

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
     * Este método dibuja el mapa conocido por el agente
     */
    public void drawMap(){
        System.out.println("| Mapa actual - Filas: " + this.mapMatrix.length + " | Columnas: " + this.mapMatrix[0].length);
        for(int i = 0; i < this.mapSize();i++) System.out.print("▉▉▉");
        System.out.println("");
        for(int i = 0; i < this.mapMatrix.length; i++){
            for (int j = 0; j < this.mapMatrix[i].length; j++) {
                int value = this.mapMatrix[i][j];
                if(j == 0) System.out.print("▉▉▉");
                if(isAnyAgentInPosition(i, j)) System.out.print(" ● ");
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
     * Devuelve el contenido en el mapa conocido del agente.
     * En caso de que la posición pedida esté fuera del conocimiento devuelve STATE_WALL
     *
     * @param px Posición X a obtener
     * @param py Posición Y a obtener
     * @return int Que contiene el contenido en las coordenadas pedidas
     */
    public int getContent(int px, int py){
        return (px < 0 || py < 0 || px > this.mapSize()  || py > this.mapSize()) ? this.STATE_WALL : this.mapMatrix[px][py];
    }


    /**
     * Método que actualiza la posición de un agente. 
     * En el caso de que ese agente no esté en la lista de agentes lo incluye
     *
     * @param agentName Nombre del agente a actualizar/añadir
     * @param posx Posición X a actualizar
     * @param posy Posición Y a actualizar
     */
    private void setAgentPosition(String agentName, int posx, int posy){
        AgentPosition aPos = new AgentPosition(agentName, posx, posy);
        int index = agentsPosition.indexOf(aPos);

        if(index != -1){
            agentsPosition.get(index).changePosition(posx, posy);
        }else{
            agentsPosition.add(aPos);
        }        
    }

    /**
     * Método para comprobar si hay un agente en una posición concreta
     *
     * @param posx Posición X a comprobar
     * @param posy Posición Y a comprobar
     * @return Devuelve true en caso de que en la posición dada exista un agente
     */
    public boolean isAnyAgentInPosition(int posx, int posy){
        boolean isInPosition = false;

        for(AgentPosition aPos: agentsPosition){
            if(aPos.isIn(posx, posy)){
                isInPosition = true;
                break;
            }
        }
        return isInPosition;
    }

    /**
     * Clase envoltorio para poder guardar todas las posiciones de los agentes actuales
     *
     * @author Samuel Peregrina Morillas
     */
    class AgentPosition {
        private String agentName;
        private Cell position;

        /**
         * Constructor por defecto de la clase
         *
         * @param name Nombre único del agente para identificarlo
         * @param position Posición del agente actualmente
         */
        public AgentPosition(String name, Cell position){
            this.agentName = name;
            this.position = position;
        }

        /**
         * Otro constructor para la clase
         *
         * @param name Nombre único del agente para identificarlo
         * @param posx Posición X que ocupa el agente
         * @param posy Posición Y que ocupa el agente
         */
        public AgentPosition(String name, int posx, int posy){
            this.agentName = name;
            this.position = new Cell(posx, posy, STATE_VEHICLE);
        }

        /**
         * Devuelve el nombre del agente
         *
         * @return El nombre del agente
         */
        public String getAgentName(){
            return this.agentName;
        }

        /**
         * Comprueba si un agente ocupa una posición específica
         * 
         * @param posx Posición X a comprobar
         * @param posy Posición Y a comprobar
         * @return true en caso de que ocupe la posición
         */
        public boolean isIn(int posx, int posy){
            return this.position.isPosition(posx, posy);
        }

        /**
        * Comprueba si es un agente concreto
        *
        * @param name Nombre a comprobar
        * @return True si es el agente a comprobar
        */
        public boolean isAgent(String name){
            return this.agentName == name;
        }

        /**
         * Cambia la posición que ocupa un agente
         *
         * @param posx Nueva posición X a ocupar
         * @param posy Nueva posición Y a ocupar
         */
        public void changePosition(int posy, int posx){
            this.position.set(posx, posy, STATE_VEHICLE);
        }

        @Override
        public boolean equals(Object object){
            if(object == null) return false;
            if(!(object instanceof AgentPosition)) return false;
            AgentPosition aPos = (AgentPosition) object;
            return this.isAgent(aPos.getAgentName());
        }
    }
}