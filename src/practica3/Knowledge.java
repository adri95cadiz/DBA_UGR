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
    private ArrayList<AgentPosition> agentsPosition = new ArrayList<>();
    private ArrayList<Cell> objetives = new ArrayList<>();

    private final static int MIN_SIDE = 100;
    private final static String DB_NAME = "knowledge";
    public final static int STATE_FREE = 0;
    public final static int STATE_WALL = 1;
    public final static int STATE_WORLD_END = 2;
    public final static int STATE_GOAL = 3;
    public final static int STATE_VEHICLE = 4;
    public final static int STATE_UNKNOWN = 5;

    /**
     * Método que devuelve la instancia de Knowledge del agente
     *
     * @param map_id El identificador del número de mapa que vamos a usar
     * @return La instancia de la BD
     */
    public static Knowledge getDB(int map_id) {
        if (instance == null) {
            instance = new Knowledge(map_id);
        } else {
            instance.setMapID(map_id);
        }
        return instance;
    }

    /**
     * Método que devuelve un Statement para realizar una acción en la BDG
     *
     * @return statement Un objeto Statement para realizar la consulta
     */
    private Statement getStatement() {
        Statement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME + ".db");
            statement = connection.createStatement();
            statement.setQueryTimeout(30);
        } catch (SQLException error) {
            System.err.println("Error en la creación de la conexión");
            System.err.println(error);
        } 
        return statement;
    }

    /**
     * Método para ejecutar una consulta en la BD Este método debe de usarse
     * cuando es una consulta única. Nunca debe de usarse dentro de un bucle ya
     * que una vez ejecuta la consulta cierra la conexión con la BD.
     *
     * @param query Consulta SQL a ejecutar
     */
    private void executeUpdate(String query) {
        try {
            Statement st = getStatement();
            st.executeUpdate(query);
            this.connection.close();
        } catch (SQLException e) {
            System.err.println("Error en la ejecución de la consulta");
            System.err.println(e);
        }
    }

    /**
     * Comprueba si un valor está contenido en la matriz
     *
     * @param value Valor a comprobar
     * @return true si está
     * @autor Raúl López Arévalo
     */
    public boolean contains(int value) {
        boolean exist_value = false;
        for (int i = 0; i < this.mapMatrix.length && !exist_value; i++) {
            for (int j = 0; j < this.mapMatrix.length && !exist_value; j++) {
                if (this.mapMatrix[i][j] == value) {
                    exist_value = true;
                }
            }
        }
        return exist_value;
    }

    /**
     * Devuelve el tamaño del mapa
     *
     * @return Tamaño del mapa
     * @autor Raúl López Arévalo
     */
    public int mapSize() {
        return this.mapMatrix.length;
    }

    /**
     * Asigna el identificador del mapa a usar
     *
     * @param id ID correspondiente al mapa a usar
     */
    private void setMapID(int id) {
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
            String creteTableSQL = "CREATE TABLE IF NOT EXISTS Mapa_" + this.map_id + " ("
                    + "pos_x INTEGER NOT NULL,"
                    + "pos_y INTEGER NOT NULL,"
                    + "contains INTEGER DEFAULT 0,"
                    + "CONSTRAINT pk_posicion PRIMARY KEY (pos_x, pos_y)"
                    + ")";

            this.executeUpdate(creteTableSQL);
            this.createMatrix();
        } catch (Exception e) {
            System.err.println("Error en la creación de la DB");
            System.err.println(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }

    /**
     * Este método es el encargado de recibir los datos obtenidos por el agente
     * y añadirlos a su conocimiento
     *
     * @param agentName Nombre del agente a actualizar
     * @param radar JsonObject que contiene la información del radar
     * @param gps JsonObject que contiene la posición del agente
     * @param vision Rago de vision del agente
     */
    public void updateStatus(String agentName, int[][] radar, Cell gps, int vision) {
        try {
            // Guardamos la posición actual del agente
            int position_x, position_y;
            position_x = gps.getPosX();
            position_y = gps.getPosY();

            //this.setAgentPosition(agentName, position_x, position_y);

            // Nos conectamos a la DB
            Statement statement = this.getStatement();

            for (int i = 0; i < vision; i++) {
                for (int j = 0; j < vision; j++) {
                    int pos_x = (position_x - (vision / 2) + j);
                    int pos_y = (position_y - (vision / 2) + i);
                    int radarValue = radar[j][i];
                    
                    if(radarValue == STATE_VEHICLE){
                        radarValue = STATE_FREE;
                    }

                    if (radarValue == Knowledge.STATE_GOAL) {
                        this.updateObjetive(new Cell(pos_x, pos_y));
                    }

                    if (pos_x >= 0 && pos_y >= 0) {
                        String querySQL = "INSERT OR REPLACE INTO Mapa_" + this.map_id + "(pos_x, pos_y, contains) VALUES("
                                + pos_x + ", "
                                + pos_y + ", "
                                + radarValue
                                + ");";
                        //Ejecutamos la consulta
                        statement.executeUpdate(querySQL);

                        //Actualizamos la fila y de la matriz
                        updateMatrix(pos_x, pos_y, radarValue);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en la actualización");
            System.err.println(e);
        } finally {
            try {
                if (connection != null) {
                    this.connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }
public void updateStatusLocal(String agentName, int[][] radar, Cell gps, int vision){
    int posx = gps.getPosX();
    int posy = gps.getPosY();
    
    for(int i=0; i<vision; i++){
        for(int j=0; j<vision; j++){
            int pos_x = (posx - (vision / 2) + j);
            int pos_y = (posy - (vision / 2) + i);
            this.mapMatrix[pos_x][pos_y] = radar[j][i];
            
        }
    }
}
    /**
     * Este método es el encargado de recibir los datos obtenidos por el agente
     * y añadirlos a su conocimiento. Método de retrocompatibilidad con la
     * versión anterior
     *
     * @deprecated
     * @param agentName Nombre del agente a actualizar
     * @param radar JsonObject que contiene la información del radar
     * @param gps JsonObject que contiene la posición del agente
     * @param vision Rago de vision del agente
     */
    public void updateStatus(String agentName, JsonObject radar, JsonObject gps, int vision) {
        int[][] radarArray = Knowledge.getRadarMatrix(radar, vision);
        Cell gpsCell = Knowledge.getGPSData(gps);

        this.updateStatus(agentName, radarArray, gpsCell, vision);
    }

    /**
     * Este método actualiza un valor de estado de una coordenada. En caso
     * necesario redimensiona la matriz.
     *
     * @param posx int Posición x de la coordenada
     * @param posy int Posición y de la coordenada
     * @param value int Valor a actualizar
     */
    private void updateMatrix(int posx, int posy, int value) {
        int maxWidth = Math.max(this.mapSize(), Math.max(posx, posy));
        
        /*System.out.println("Máximo actual anterior: " + this.mapSize());
        System.out.println("Valor X: " + posx + " | Valor Y: " + posy);*/

        /*if (maxWidth > this.mapSize()) {
            int[][] tmp = this.mapMatrix;
            this.mapMatrix = new int[maxWidth][maxWidth];

            for(int i = 0; i < maxWidth; i++){
                for(int j = 0; j < this.mapSize(); j++){
                    this.mapMatrix[i][j] = Knowledge.STATE_UNKNOWN;
                }
            }

            for (int i = 0; i < tmp.length; i++) {
                for (int j = 0; j < tmp[i].length; j++) {
                    this.mapMatrix[i][j] = tmp[i][j];
                }
            }
        }*/
        if(maxWidth > this.mapSize()) {
            int[][] tmp = new int[maxWidth+1][maxWidth+1];
            
            for(int i = 0; i < maxWidth+1; i++){
                for(int j = 0; j < maxWidth+1; j++){
                    tmp[i][j] = Knowledge.STATE_UNKNOWN;
                }
            }
            for (int i = 0; i < tmp.length; i++) {
                for (int j = 0; j < tmp[i].length; j++) {
                    tmp[i][j] = this.mapMatrix[i][j];
                }
            }
            this.mapMatrix = tmp;
        }
        //System.out.println("Máximo actual posterior: " + this.mapSize());
        this.mapMatrix[posx][posy] = value;
    }

    /**
     * Este método se encarga de crear la matriz, si tiene datos carga los
     * datos. En el caso de que todavía no hubiese datos inicializa la matriz
     * con valores por defecto.
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
            while (rs.next()) {
                matrix_size = rs.getInt("count");
            }

            System.out.println("\nCantidad de celdas conocidas: " + matrix_size);

            if (matrix_size > 0) {
                matrix_size = 0;
                // Calculamos el tamaño máximo de la matriz
                // Por posición X
                sqlCount = "SELECT MAX(pos_x) AS count FROM Mapa_" + this.map_id + ";";

                rs = statement.executeQuery(sqlCount);
                while (rs.next()) {
                    matrix_size = Math.max(matrix_size, (rs.getInt("count") + 1));
                }

                // Por posición Y
                sqlCount = "SELECT MAX(pos_y) AS count FROM Mapa_" + this.map_id + ";";

                rs = statement.executeQuery(sqlCount);
                while (rs.next()) {
                    matrix_size = Math.max(matrix_size, (rs.getInt("count") + 1));
                }

                System.out.println("El máximo de la matriz es: " + matrix_size);

                // Creamos la matriz con el tamaño conocido
                this.mapMatrix = new int[matrix_size][matrix_size];
                for(int i = 0; i < this.mapSize(); i++){ 
                    for(int j = 0; j < this.mapSize(); j++){ 
                        this.mapMatrix[i][j] = Knowledge.STATE_UNKNOWN; 
                    }
                } 
                // Obtenemos la información almacenada y la volcamos en la matriz
                rs = statement.executeQuery("SELECT * FROM Mapa_" + this.map_id + ";");
                while (rs.next()) {
                    int contain = rs.getInt("contains");
                    int pos_x = rs.getInt("pos_x");
                    int pos_y = rs.getInt("pos_y");

                    if (contain == Knowledge.STATE_GOAL) {
                        this.updateObjetive(new Cell(pos_x, pos_y));
                    }
                    this.mapMatrix[pos_x][pos_y] = contain;
                }
            } else {
                this.mapMatrix = new int[MIN_SIDE][MIN_SIDE];
                for(int i=0; i < MIN_SIDE; i++){
                    for(int j=0; j < MIN_SIDE; j++){
                        this.mapMatrix[i][j] = Knowledge.STATE_UNKNOWN;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e);
            }
        }
    }

    /**
     * Genera el mapa contenido en la matriz
     */
    public static String drawMapToString(int[][] matrix) {
        String output = "";        
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) { 
                  int value = matrix[i][j];   
                  if(j == 0) output += "▉▉▉"; 
                  switch (value) { 
                      case Knowledge.STATE_FREE: 
                          output += "0"; 
                          break; 
                      case Knowledge.STATE_WALL: 
                          output += "#"; 
                          break; 
                      case Knowledge.STATE_GOAL: 
                          output += "X"; 
                          break; 
                      case Knowledge.STATE_WORLD_END: 
                          output += "#"; 
                          break; 
                      case Knowledge.STATE_UNKNOWN: 
                          output += "?"; 
                          break;  
                      case Knowledge.STATE_VEHICLE: 
                          output += "A"; 
                          break;                      
                  } 
              } 
              output += "\n"; 
        }
        return output;
    }

    /**
     * Genera el mapa conocido en Knowledge
     */
    public String drawMapToString(){
        return Knowledge.drawMapToString(getKnowledgeMatrix());
    }
    
    /**
     * Dibuja del mapa guardado en la BD 
     */
    public void drawDBMap() {
        try{
            Statement st = getStatement();
            ResultSet rs;
            int matrix_size = 0;

            // Calculamos el tamaño de la matriz
            String query = "SELECT MAX(pos_x) as max_x, MAX(pos_y) AS max_y FROM Mapa_" + this.map_id + ";";

            rs = st.executeQuery(query);
            while (rs.next()) {
                matrix_size = Math.max(rs.getInt("max_x"), rs.getInt("max_y")) + 1;
            }

            // Creamos una matriz vacia con STATE_UNKNOWN por defecto
            int[][] drawMatrix = new int[matrix_size][matrix_size];
            for(int i = 0; i < matrix_size; i++){ 
                for(int j = 0; j < matrix_size; j++){ 
                    drawMatrix[i][j] = Knowledge.STATE_UNKNOWN; 
                }
            } 

            // Insertamos los valores guardados en la BD en la matriz
            rs = st.executeQuery("SELECT * FROM Mapa_" + this.map_id + ";");
            while (rs.next()) {
                int contain = rs.getInt("contains");
                int pos_x = rs.getInt("pos_x");
                int pos_y = rs.getInt("pos_y");

                drawMatrix[pos_x][pos_y] = contain;
            }
            this.drawMap(drawMatrix);
        }catch(SQLException e){
            
        }
    }    
    
    /**
     * Dibuja el mapa en consola
     * 
     * @param matrix Matriz que se quiere dibujar
     */
    public void drawMap(int[][] matrix) {
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println("| Mapa " + this.map_id + " | Filas: " + this.mapMatrix.length + " | Columnas: " + this.mapMatrix[0].length + " |");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        System.out.println("/////////////////////////////////////////////////////////////////////////////////////////////////////");
        System.out.println(drawMapToString(matrix));
        System.out.println("/////////////////////////////////////////////////////////////////////////////////////////////////////");    
    }

    /**
     * Dibuja el mapa de Knowledge en consola
     */
    public void drawMap(){
        this.drawMap(getKnowledgeMatrix());
    }

    /**
     * Devuelve el contenido en el mapa conocido del agente. En caso de que la
     * posición pedida esté fuera del conocimiento devuelve STATE_WALL
     *
     * @param px Posición X a obtener
     * @param py Posición Y a obtener
     * @return int Que contiene el contenido en las coordenadas pedidas
     */
    public int getContent(int px, int py) {
        return (px < 0 || py < 0 || px > this.mapSize() || py > this.mapSize()) ? this.STATE_UNKNOWN : this.mapMatrix[px][py];
    }

    /**
     * Devuelve una copia de la matriz de Knowledge incluyendo los todos los
     * vehículos
     *
     * @return Matriz copia de la matriz almacenada en Knowledge
     */
    public int[][] getKnowledgeMatrix() {
        int[][] cloneMatrix = new int[mapMatrix.length][mapMatrix[0].length];
        for (int i = 0; i < mapMatrix.length; i++){
            for (int j = 0; j < mapMatrix[i].length; j++){
                cloneMatrix[i][j] = mapMatrix[i][j];
            }
        }
        for (AgentPosition ap : this.agentsPosition) {
            if(ap.getPosition().getPosX() < this.mapMatrix.length && ap.getPosition().getPosY() < this.mapMatrix[0].length)
                cloneMatrix[ap.getPosition().getPosX()][ap.getPosition().getPosY()] = STATE_VEHICLE;
        }
        return cloneMatrix;
    }
    
    /**
     * Devuelve una copia de la matriz de Knowledge poniendo los obstáculos a -1 y los caminos a 0
     *
     * @return Matriz copia de la matriz almacenada en Knowledge
     */
    public int[][] getPathMatrix() {
        int[][] cloneMatrix = new int[mapMatrix.length][mapMatrix[0].length];
        for (int i = 0; i < mapMatrix.length; i++){
            for (int j = 0; j < mapMatrix[i].length; j++){
                cloneMatrix[i][j] = mapMatrix[i][j];
            }
        }
        for (int i = 0; i < cloneMatrix.length; i++){
            for (int j = 0; j < cloneMatrix[i].length; j++){
                if(cloneMatrix[i][j] == STATE_FREE || cloneMatrix[i][j] == STATE_GOAL)
                    cloneMatrix[i][j] = 0;
                else
                    cloneMatrix[i][j] = -1;
            }
        }
        for (AgentPosition ap : this.agentsPosition) {
            cloneMatrix[ap.getPosition().getPosX()][ap.getPosition().getPosY()] = -1;
        }
        return cloneMatrix;
    }
    

    /**
     * Método que actualiza la posición de un agente. En el caso de que ese
     * agente no esté en la lista de agentes lo incluye
     *
     * @param agentName Nombre del agente a actualizar/añadir
     * @param posx Posición X a actualizar
     * @param posy Posición Y a actualizar
     */
    public void setAgentPosition(String agentName, int posx, int posy) {
        AgentPosition aPos = new AgentPosition(agentName, posx, posy);
        boolean encontrado = false;
        for (AgentPosition ap : this.agentsPosition) {
            if(ap.getAgentName().equals(agentName)){                
                ap.changePosition(posx, posy);
                System.out.println("Cambia posicion agente");
                encontrado = true;
            } 
        }
        if(encontrado == false){
            System.out.println("Crea nueva posicion");
            agentsPosition.add(new AgentPosition(agentName, new Cell(posx,posy,4)));
        }
    }

    /**
     * Método para comprobar si hay un agente en una posición concreta
     *
     * @param posx Posición X a comprobar
     * @param posy Posición Y a comprobar
     * @return Devuelve true en caso de que en la posición dada exista un agente
     */
    public boolean isAnyAgentInPosition(int posx, int posy) {
        boolean isInPosition = false;

        for (AgentPosition aPos : agentsPosition) {
            if (aPos.isIn(posx, posy)) {
                isInPosition = true;
                break;
            }
        }
        return isInPosition;
    }

    /**
     * Actualiza los objetivos
     *
     * @param objetive Celda en la que se encuentra el objetivo
     */
    private void updateObjetive(Cell objetive) {
        if (!objetives.contains(objetive)) {
            objetives.add(objetive);
        }
    }

    /**
     * Devuelve una lista de los objetivos encontrados
     *
     * @return Un {@link ArrayList} con los objetivos
     */
    public ArrayList<Cell> getObjetives() {
        return objetives;
    }

    /**
     * Convierte el el objeto json gps a un objeto Cell
     *
     * @param gps JsonObject que contiene los datos del gps
     * @return 
     */
    public static Cell getGPSData(JsonObject gps) {
        Cell position = new Cell();

        JsonObject gpsObject = gps.get("gps").asObject();
        position.set(gpsObject.get("x").asInt(), gpsObject.get("y").asInt(), -1);

        return position;
    }

    /**
     * Convierte el el objeto json radar en un {@link ArrayList} de enteros
     *
     * @param radar
     * @return 
     */
    public static ArrayList<Integer> getRadarData(JsonObject radar) {
        JsonArray radarJson = radar.get("radar").asArray();
        ArrayList<Integer> radarMatrix = new ArrayList<>();

        for (int i = 0; i < radarJson.size(); i++) {
            radarMatrix.add(radarJson.get(i).asInt());
        }

        return radarMatrix;
    }

    /**
     * Convierte el el objeto json radar en una matriz de enteros
     *
     * @param radar
     * @param vision
     * @return 
     */
    public static int[][] getRadarMatrix(JsonObject radar, int vision) {
        ArrayList<Integer> radarArray = Knowledge.getRadarData(radar);
        int[][] matrix = new int[vision][vision];

        for (int i = 0; i < vision; i++) {
            for (int j = 0; j < vision; j++) {
                matrix[i][j] = radarArray.get((j * vision) + i);
            }
        }
        return matrix;
    }

    /**
     * Clase envoltorio para poder guardar todas las posiciones de los agentes
     * actuales
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
        public AgentPosition(String name, Cell position) {
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
        public AgentPosition(String name, int posx, int posy) {
            this.agentName = name;
            this.position = new Cell(posx, posy, STATE_VEHICLE);
        }

        /**
         * Devuelve el nombre del agente
         *
         * @return El nombre del agente
         */
        public String getAgentName() {
            return this.agentName;
        }

        /**
         * Devuelve la posición del Agente
         *
         * @return Cell correspondiente a la posición del agente
         */
        public Cell getPosition() {
            return position;
        }

        /**
         * Comprueba si un agente ocupa una posición específica
         *
         * @param posx Posición X a comprobar
         * @param posy Posición Y a comprobar
         * @return true en caso de que ocupe la posición
         */
        public boolean isIn(int posx, int posy) {
            return this.position.isPosition(posx, posy);
        }

        /**
         * Comprueba si es un agente concreto
         *
         * @param name Nombre a comprobar
         * @return True si es el agente a comprobar
         */
        public boolean isAgent(String name) {
            return this.agentName == name;
        }

        /**
         * Cambia la posición que ocupa un agente
         *
         * @param posx Nueva posición X a ocupar
         * @param posy Nueva posición Y a ocupar
         */
        public void changePosition(int posy, int posx) {
            this.position.set(posx, posy, STATE_VEHICLE);
        }

        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (!(object instanceof AgentPosition)) {
                return false;
            }
            AgentPosition aPos = (AgentPosition) object;
            return this.isAgent(aPos.getAgentName());
        }
    }
}
