package practica3;


/**
 * Clase que almacena las matrices locales de los agentes e incorpora métodos
 * helpers trabajar con ellas
 *
 * @author Samuel Peregrina Morillas
 * @version 2017.01.12
 */
class GugelVehicleMatrix {

    private Vehicle vehicle;
    private Knowledge db;

    /**
     * Constructor de la clase GugelVehicle
     *
     * @param db Objeto {@link Knowledge} a utilizar
     */
    public GugelVehicleMatrix(Knowledge db, String name, int alcance) {
        this.db = db;
        this.vehicle = new Vehicle(name, alcance);
    }

   /**
     * @author Raúl López Arévalo
     */
    public String ImprimirKnow() {
        return this.db.drawMapToString();
    }

    /**
     * Devuelve la matriz de Knowledge
     *
     * @return Matriz de {@link Knowledge}
     */
    public int[][] getKnowledgeMatrix() {
        return this.db.getKnowledgeMatrix();
    }

    /**
     * Devuelve la matriz local del agente
     *
     * @param agentName Nombre del agente del que se va a pedir su matriz
     * @return Matriz local del agente; {@link null} en caso de no existir el
     * agente
     */
    public int[][] getLocalMatrix() {
        return getVehicle().getLocalMatrix();
    }

    /**
     * @author Raúl López Arévalo
     */
    public void ImprimirLocal() {
        int[][] m = getVehicle().getLocalMatrix();
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m.length; j++) {
                System.out.print(m[i][j] + " ");
            }
            System.out.print("\n");
        }
    }
    
    /**
     * *************************************************************************
     * Dibuja la matriz de mapa y la devuelve como String.
     * 
     */
    public String drawMapToString(){
        String output = "";
        int[][] m = getVehicle().getCombinedKnowledge();
        for(int i = 0; i < m.length; i++){
            for (int j = 0; j < m.length; j++) {
                int value = m[i][j];
                if(j == 0) output += i+"▉▉▉";
                else{
                    switch (value) {
                        case Knowledge.STATE_FREE:
                            output += "0";
                            break;
                        case Knowledge.STATE_VEHICLE:
                            output += "A";
                            break;
                        case Knowledge.STATE_WALL:
                            output += "#";
                            break;
                        case Knowledge.STATE_GOAL:
                            output += ">";
                            break;
                        case Knowledge.STATE_WORLD_END:
                            output += "$";
                            break;
                        case Knowledge.STATE_UNKNOWN:
                            if(value > 0 )
                                output += "?";
                            else                                
                                output += "+";
                            break; 
                        default:
                            if(value > 0 )
                                output += "?";
                            else                                
                                output += "+";
                            break;  
                    }
                }
            }
            output += "\n";
        }
        return output;
    }
    /**
     * @author Raúl López Arévalo
     */
    /**
     * Devuelve una matriz resultante de combinar la matriz de {@link Knowledge}
     * y la matriz local del agente almacenada en {@link Vehicle}
     *
     * @param agentName Nombre del agente del que se va a pedir su matriz
     * @return Matriz combinada del agente; {@link null} en caso de no existir
     * el agente
     */
    public int[][] getCombinedKnowledge() {
        return getVehicle().getCombinedKnowledge();
    }

    /**
     * 
     * @author Raúl López Arévalo
     *   imprimir getcombineknowledge
     */
    public void ImprimirGetCombined() {
        int[][] m = getVehicle().getCombinedKnowledge();
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m.length; j++) {
                System.out.print(m[i][j] + " ");
            }
            System.out.print("\n");
        }
    }

    /**
     * Actualiza los datos del agente
     *
     * @param agentName Nombre del agente a actualizar
     * @param radar ArrayList correspondiente al radar
     * @param gps Cell que contiene la posición actual
     */
    public void updateMatrix(int[][] radar, Cell gps) {
        this.getVehicle().updateAgent(radar, gps);
    }

    /**
     * Devuelve un {@link Vehicle} almacenado en la instancia
     *
     * @param agentName Nombre del agente a buscar
     * @return Objeto {@link Vehicle} en caso de que exista, {@link null} cuando
     * no existe
     */
    private Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * Clase Vehicle utilizada por {@link GugelVehicleMatrix}
     *
     * @author Samuel Peregrina Morillas
     */
    private class Vehicle {

        /**
         * Matriz local del agente
         */
        private int[][] localMatrix;

        /**
         * Turno actual del agente
         */
        private int turn;

        /**
         * Rango de visión del agente
         */
        private int vision;

        /**
         * Posición actual del agente
         */
        private Cell position;

        /**
         * Nombre del agente
         */
        private String agentName;

        /**
         * Contructor por defecto de Vehicle
         */
        public Vehicle(String agentName, int vision) {
            this.turn = 0;
            this.vision = vision;
            this.agentName = agentName;
            this.localMatrix = new int[db.mapSize()][db.mapSize()];
        }

        /**
         * Actualiza los datos del agente
         *
         * @param radar Objeto radar que contiene la visualización del agente
         * @param gps Objeto gps que contiene las coordenadas del agente
         */
        public void updateAgent(int[][] radar, Cell gps) {
            db.updateStatus(agentName, radar, gps, vision);
            //db.updateStatusLocal(agentName, radar,gps, vision);
            this.turn++;
            this.position = gps;
            this.updateLocalMatrix(radar);
        }

        /**
         * Actualiza las posiciones de la matriz local
         *
         * @param radar Objeto radar que contiene los datos a actualizar en la
         * matriz
         */
        private void updateLocalMatrix(int[][] radar) {
            // Compara tamaño con el mapa BD y lo pone al mismo tamaño.
            if (this.localMatrix.length < db.mapSize()) {
                int[][] tmp = this.localMatrix;

                this.localMatrix = new int[db.mapSize()][db.mapSize()];
                for (int i = 0; i < tmp.length; i++) {
                    for (int j = 0; j < tmp[i].length; j++) {
                        this.localMatrix[i][j] = tmp[i][j];
                    }
                }
            }
            int max = radar.length-1;
            for (int i = 1; i < radar.length-1; i++) {
                for (int j = 1; j < radar.length-1; j++) {
                    int pos_x = (position.getPosX() - ((vision - 1) / 2) + i);
                    int pos_y = (position.getPosY() - ((vision - 1) / 2) + j);
                    int radarValue = radar[i][j];
                    
                    //if( (i == 0 && j == 0) || (i==max && j==max) || (j == 0 && i == max) || (j==max && i==0) ){
                        
                    //}
                    //else if(radarValue != Knowledge.STATE_WALL && radarValue != Knowledge.STATE_WORLD_END)
                    //{
                        this.localMatrix[pos_x][pos_y] = -turn;
                    //}
                }
            }
        }

        /**
         * Devuelve la matriz local del Agente
         *
         * @return Matriz local del agente
         */
        public int[][] getLocalMatrix() {
            return this.localMatrix;
        }

        /**
         * Devuelve una combinación de la matriz local del agente y de la matriz
         * de Knowledge.
         *
         * Esta matriz está compuesta por número positivos que hacen referencia
         * a estados del mapa y datos negativos, que hacen referencia a la
         * última vez que el agente estuvo en la celda
         *
         * @return matriz combinada
         */
        public int[][] getCombinedKnowledge() {
            int[][] map = db.getKnowledgeMatrix();
            for (int i = 0; i < db.mapSize(); i++) {
                for (int j = 0; j < db.mapSize(); j++) {
                    if (map[i][j] == db.STATE_FREE && this.localMatrix[i][j] < 0) {
                        map[i][j] = this.localMatrix[i][j];
                    }
                }
            }
            return map;
        }

        /**
         * Devuelve el turno actual del agente
         *
         * @return Entero positivo correspondiente con el turno actual
         */
        public int getTurn() {
            return turn;
        }

        /**
         * Comprueba si el nombre del agente coincide con el {@link Vehicle}
         *
         * @param agentName Nombre del agente a comprobar
         * @return Devuelve {@link true} si coincide con el nombre del agente
         */
        public boolean isAgent(String agentName) {
            return agentName == this.agentName;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (!(object instanceof Vehicle)) {
                return false;
            }
            Vehicle vehicle = (Vehicle) object;
            return this.isAgent(vehicle.agentName);
        }
    }
}
