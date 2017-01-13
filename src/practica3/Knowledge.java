package practica3;

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
    public final int STATE_UNKNOW = 5;

    /**
     * MÃ©todo que devuelve la instancia de Knowledge del agente
     *
     * @param map_id El identificador del nÃºmero de mapa que vamos a usar
     * @return La instancia de la BD
     */
    public static Knowledge getDB(int map_id){
        if(instance == null){
            instance = new Knowledge(map_id);
        }else{ instance.setMapID(map_id); }
        return instance;
    }

    /**
     * MÃ©todo que devuelve un Statement para realizar una acciÃ³n en la BDG
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
            System.err.println("Error en la creaciÃ³n de la conexiÃ³n");
            System.err.println(error);
        }finally{
            return statement;
        }
    }

    /**
     * MÃ©todo para ejecutar una consulta en la BD
     * Este mÃ©todo debe de usarse cuando es una consulta Ãºnica.
     * Nunca debe de usarse dentro de un bucle ya que una vez ejecuta la consulta 
     * cierra la conexiÃ³n con la BD.
     *
     * @param query Consulta SQL a ejecutar
     */
    private void executeUpdate(String query){
        try{
            Statement st = getStatement();
            st.executeUpdate(query);
            this.connection.close();
        }catch(SQLException e){
            System.err.println("Error en la ejecuciÃ³n de la consulta");
            System.err.println(e);
        }
    }

    /**
     * Comprueba si un valor estÃ¡ contenido en la matriz
     * @param value Valor a comprobar
     * @return true si estÃ¡
     * @autor RaÃºl LÃ³pez ArÃ©valo
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
     * Devuelve el tamaÃ±o del mapa
     *
     * @return TamaÃ±ao del mapa
     * @autor RaÃºl LÃ³pez ArÃ©valo
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
            System.err.println("Error en la creaciÃ³n de la DB");
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
     *  Este mÃ©todo es el encargado de recibir los datos obtenidos por el agente y
     *  aÃ±adirlos a su conocimiento
     *
     * @param radar JsonObject que contiene la informaciÃ³n del radar
     * @param gps JsonObject que contiene la posiciÃ³n del agente
     * @param vision Rago de vision del agente
     */
    public void updateStatus(String agentName, JsonObject radar, JsonObject gps, int vision) {
        try{
            // Guardamos la posiciÃ³n actual del agente
            int position_x, position_y;
            Cell position = Knowledge.getGPSData(gps);
            position_x = position.getPosX();
            position_y = position.getPosY();

            this.setAgentPosition(agentName, position_x, position_y);

            //Transformamos el array JSON del radar a un array de int
            JsonArray radarJson = radar.get("radar").asArray();
            ArrayList<Integer> radarMatrix = new ArrayList<>();

            for (int i = 0; i < radarJson.size(); i++) {
                radarMatrix.add(radarJson.get(i).asInt());
            }

            // Nos conectamos a la DB
            Statement statement = this.getStatement();

            for (int i = 0; i < vision; i++) {
                for (int j = 0; j < vision; j++) {
                    int pos_x = (position_x -(vision/2) + j);
                    int pos_y = (position_y -(vision/2) + i);
                    int radarValue = radarMatrix.get(j*vision + i);

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
            System.err.println("Error en la actualizaciÃ³n");
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
     * Este mÃ©todo actualiza un valor de estado de una coordenada.
     * En caso necesario redimensiona la matriz.
     *
     * @param posx int PosiciÃ³n x de la coordenada
     * @param posy int PosiciÃ³n y de la coordenada
     * @param value int Valor a actualizar
     */
    private void updateMatrix(int posx, int posy, int value){
        int maxWidth = Math.max(this.mapSize(), Math.max(posx, posy));

        if(maxWidth > this.mapSize()){
            int[][] tmp = this.mapMatrix;
            this.mapMatrix = new int[maxWidth][maxWidth];
            for(int i = 0; i < tmp.length; i++){
                for(int j = 0; j < tmp[i].length; j++){
                    this.mapMatrix[i][j] = tmp[i][j];
                }
            }
        }

        this.mapMatrix[posx][posy] = value;
    }

    /**
     * Este mÃ©todo se encarga de crear la matriz, si tiene datos carga los datos.
     * En el caso de que todavÃ­a no hubiese datos inicializa la matriz con valores por defecto.
     */
    private void createMatrix() {
        try {
            String output = "";
            int matrix_size = 0;
            // Nos conectamos a la DB
            Statement statement = this.getStatement();

            String sqlCount;
            ResultSet rs;

            // Comprobamos si ya hay informaciÃ³n del mapa
            sqlCount = "SELECT COUNT(*) AS count FROM Mapa_" + this.map_id + ";";

            rs = statement.executeQuery(sqlCount);
            while(rs.next()){
                matrix_size = rs.getInt("count");
            }

            output.concat("\nCantidad de celdas conocidas: " + matrix_size);

            if(matrix_size > 0) {
                matrix_size = 0;
                // Calculamos el tamaÃ±o mÃ¡ximo de la matriz
                // Por posiciÃ³n X
                sqlCount = "SELECT MAX(pos_x) AS count FROM Mapa_" + this.map_id + ";";

                rs = statement.executeQuery(sqlCount);
                while(rs.next()){
                    matrix_size = Math.max(matrix_size, (rs.getInt("count") + 1));
                }

                // Por posiciÃ³n Y
                sqlCount = "SELECT MAX(pos_y) AS count FROM Mapa_" + this.map_id + ";";

                rs = statement.executeQuery(sqlCount);
                while(rs.next()){
                    matrix_size = Math.max(matrix_size, (rs.getInt("count") + 1));
                }

                output.concat("El mÃ¡ximo de la matriz es: " + matrix_size);

                // Creamos la matriz con el tamaÃ±o conocido
                this.mapMatrix = new int[matrix_size][matrix_size];

                // Obtenemos la informaciÃ³n almacenada y la volcamos en la matriz
                rs = statement.executeQuery("SELECT * FROM Mapa_"+this.map_id +";");
                while(rs.next()){
                    this.mapMatrix[rs.getInt("pos_x")][rs.getInt("pos_y")] = rs.getInt("state");
                }
            }else{
                output.concat("Creamos matriz desde cero");

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
     * Genera el mapa conocido por el agente
     */
    public String drawMapToString(){
        String output = "";
        for(int i = 0; i < this.mapSize();i++) output.concat(" â–‰â–‰â–‰");
        output.concat("");
        for(int i = 0; i < this.mapMatrix.length; i++){
            for (int j = 0; j < this.mapMatrix[i].length; j++) {
                int value = this.mapMatrix[i][j];
                if(j == 0) output.concat("â–‰â–‰â–‰");
                if(isAnyAgentInPosition(i, j)) output.concat(" â—� ");
                else{
                    switch (value) {
                        case 0:
                            output.concat(" âŽ• ");
                            break;
                        case -1:
                            output.concat("â–‰â–‰â–‰");
                            break;
                        case -2:
                            output.concat(" â•³ ");
                            break;
                        default:
                            if(value < 10) output.concat(" " + value+ " ");
                            else if(value < 100) output.concat(" " + value);
                            else output.concat(value+"");
                            break;
                    }
                }
            }
            output.concat("\n");
        }
        return output;
    }

    /**
     * Dibuja el mapa en consola
     */
    public void drawMap(){
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println("| Mapa " + this.map_id + " | Filas: " + this.mapMatrix.length + " | Columnas: " + this.mapMatrix[0].length + " |");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println(drawMapToString());
        System.out.println("/////////////////////////////////////////////////////////////////////////////////////////////////////");
    }

    /**
     * Devuelve el contenido en el mapa conocido del agente.
     * En caso de que la posiciÃ³n pedida estÃ© fuera del conocimiento devuelve STATE_WALL
     *
     * @param px PosiciÃ³n X a obtener
     * @param py PosiciÃ³n Y a obtener
     * @return int Que contiene el contenido en las coordenadas pedidas
     */
    public int getContent(int px, int py){
        return (px < 0 || py < 0 || px > this.mapSize()  || py > this.mapSize()) ? this.STATE_UNKNOW : this.mapMatrix[px][py];
    }

    /**
     * Devuelve una copia de la matriz de Knowledge incluyendo los todos los vehÃ­culos
     * 
     * @return Matriz copia de la matriz almacenada en Knowledge
     */
    public int[][] getKnowledgeMatrix(){
        int[][] cloneMatrix = mapMatrix.clone();
        for(AgentPosition ap: this.agentsPosition){
            cloneMatrix[ap.getPosition().getPosX()][ap.getPosition().getPosY()] = STATE_VEHICLE;
        }
        return cloneMatrix;
    }


    /**
     * MÃ©todo que actualiza la posiciÃ³n de un agente. 
     * En el caso de que ese agente no estÃ© en la lista de agentes lo incluye
     *
     * @param agentName Nombre del agente a actualizar/aÃ±adir
     * @param posx PosiciÃ³n X a actualizar
     * @param posy PosiciÃ³n Y a actualizar
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
     * MÃ©todo para comprobar si hay un agente en una posiciÃ³n concreta
     *
     * @param posx PosiciÃ³n X a comprobar
     * @param posy PosiciÃ³n Y a comprobar
     * @return Devuelve true en caso de que en la posiciÃ³n dada exista un agente
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

    public static Cell getGPSData(JsonObject gps){
        Cell position = new Cell();

        JsonObject gpsObject = gps.get("gps").asObject();
        position.set(gpsObject.get("x").asInt(), gpsObject.get("y").asInt(), -1);

        return position;
    }

    public static ArrayList<Integer> getRadarData(JsonObject radar){
        JsonArray radarJson = radar.get("radar").asArray();
        ArrayList<Integer> radarMatrix = new ArrayList<>();

        for (int i = 0; i < radarJson.size(); i++) {
            radarMatrix.add(radarJson.get(i).asInt());
        }

        return radarMatrix;
    }

    public static int[][] getRadarMatrix(JsonObject radar, int vision){
        ArrayList<Integer> radarArray = Knowledge.getRadarData(radar);
        int[][] matrix = new int[vision][vision];

        for (int i = 0; i < vision; i++) {
            for(int j = 0; j < vision; j++){

            }            
        }
        return matrix;
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
         * @param name Nombre Ãºnico del agente para identificarlo
         * @param position PosiciÃ³n del agente actualmente
         */
        public AgentPosition(String name, Cell position){
            this.agentName = name;
            this.position = position;
        }

        /**
         * Otro constructor para la clase
         *
         * @param name Nombre Ãºnico del agente para identificarlo
         * @param posx PosiciÃ³n X que ocupa el agente
         * @param posy PosiciÃ³n Y que ocupa el agente
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
         * Devuelve la posiciÃ³n del Agente
         * @return Cell correspondiente a la posiciÃ³n del agente
         */
        public Cell getPosition(){
            return position;
        }

        /**
         * Comprueba si un agente ocupa una posiciÃ³n especÃ­fica
         * 
         * @param posx PosiciÃ³n X a comprobar
         * @param posy PosiciÃ³n Y a comprobar
         * @return true en caso de que ocupe la posiciÃ³n
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
         * Cambia la posiciÃ³n que ocupa un agente
         *
         * @param posx Nueva posiciÃ³n X a ocupar
         * @param posy Nueva posiciÃ³n Y a ocupar
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
