package practica3;

import com.eclipsesource.json.JsonObject;
import java.util.ArrayList;

/**
 * Clase que almacena las matrices locales de los agentes e incorpora métodos helpers trabajar con ellas
 * 
 * @author Samuel Peregrina Morillas
 * @version 2017.01.12
 */
class GugelVehicleMatrix {
    private int map_id;
    private ArrayList<Vehicle> vehicles;
    private Knowledge db;

    /**
     * Constructor de la clase GugelVehicle
     *
     * @param db Objeto {@link Knowledge} a utilizar
     */
    public GugelVehicleMatrix (Knowledge db) {
        this.db = db;
        this.vehicles = new ArrayList<Vehicle>();
    }

    /**
     * Devuelve la matriz de Knowledge
     *
     * @return Matriz de {@link Knowledge}
     */
    public int[][] getKnowledgeMatrix(){
        return this.db.getKnowledgeMatrix();
    }

    /**
     * Devuelve la matriz local del agente
     *
     * @param agentName Nombre del agente del que se va a pedir su matriz
     * @return Matriz local del agente; {@link null} en caso de no existir el agente
     */ 
    public void getLocalMatrix(String agentName){
        getVehicle(agentName).getLocalMatrix();
    }

    /**
     * Devuelve una matriz resultante de combinar la matriz de {@link Knowledge} y
     * la matriz local del agente almacenada en {@link Vehicle}
     *
     * @param agentName Nombre del agente del que se va a pedir su matriz
     * @return Matriz combinada del agente; {@link null} en caso de no existir el agente
     */ 
    public void getCombinedKnowledge(String agentName){
        getVehicle(agentName).getCombinedKnowledge();
    }

    /**
     * Devuelve un {@link Vehicle} almacenado en la instancia
     *
     * @param agentName Nombre del agente a buscar
     * @return Objeto {@link Vehicle} en caso de que exista, {@link null} cuando no existe
     */
    private Vehicle getVehicle(String agentName){
        for(Vehicle v: vehicles){
            if(v.isAgent(agentName)) return  v;
        }
        return null;
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

        public Vehicle(String agentName, int vision){
            this.turn = 0;
            this.vision = vision;
            this.agentName = agentName;
            this.localMatrix = new int[db.mapSize()][db.mapSize()];
        }

        public void updateAgent(JsonObject radar, JsonObject gps){
            db.updateStatus(agentName, radar, gps, vision);
            this.turn++;
            this.position = Knowledge.getGPSData(gps);
            this.updateLocalMatrix(radar);

        }

        private void updateLocalMatrix(JsonObject radar){
            ArrayList<Integer> radarArray = Knowledge.getRadarData(radar);
        
            if(this.localMatrix.length < db.mapSize()){
                int[][] tmp = this.localMatrix;

                this.localMatrix = new int[db.mapSize()][db.mapSize()];
                for(int i = 0; i < tmp.length; i++){
                    for(int j = 0; j < tmp[i].length; j++){
                        this.localMatrix[i][j] = tmp[i][j];
                    }
                }
            }


            for (int i = 0; i < vision; i++) {
                for (int j = 0; j < vision; j++) {
                    int pos_x = (position.getPosX() -(vision/2) + j);
                    int pos_y = (position.getPosY() -(vision/2) + i);
                    int radarValue = radarArray.get(j*vision + i);

                    if(pos_x >= 0 && pos_y >= 0){
                        this.localMatrix[pos_x][pos_y] = radarValue;
                    }
                }
            }
        }

        /**
         * Devuelve la matriz local del Agente
         */
        public int[][] getLocalMatrix(){
            return this.localMatrix;
        }

        /**
         * Devuelve una combinación de la matriz local del agente y 
         * de la matriz de Knowledge
         */
        public Cell[][] getCombinedKnowledge(){
            return null;
        }

        /**
         * Comprueba si el nombre del agente coincide con el {@link Vehicle}
         *
         * @param agentName Nombre del agente a comprobar
         * @return Devuelve {@link true} si coincide con el nombre del agente
         */
        public boolean isAgent(String agentName){
            return agentName == this.agentName;
        }

        @Override
        public boolean equals(Object object){
            if(object == null) return false;
            if(!(object instanceof Vehicle)) return false;
            Vehicle vehicle = (Vehicle) object;
            return this.isAgent(vehicle.agentName);
        }
    }
}