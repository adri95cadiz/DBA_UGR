package gugelcar;

import java.util.ArrayList;

/**
 *
 * @author Raúl López Arévalo
 * 
 * Clase que calcula el camino óptimo desde una posición A a otra B en un mapa 
 * utilizando el algoritmo A*.
 * 
 */
public class Path {

    private ArrayList<Integer> path_objetive = new ArrayList<>();
    private ArrayList<Node> open_list = new ArrayList<>();
    private ArrayList<Node> close_list = new ArrayList<>();
    private int objetive_id;
    private int start_id;
    private int[][] map;
    /**
     * Constructor
     * 
     * @param map Mapa
     * @param agent_id ID posición actual
     * @param objetive_id ID posición destino
     */
    public Path(int[][] map, int agent_id, int objetive_id) {
        this.map = map;
        this.map[objetive_id / 5][objetive_id % 5] = 2;
        this.start_id = agent_id;
        this.objetive_id = objetive_id;
    }
    /**
     * Actualiza el ID del objetivo
     * 
     * @param node_id ID nuevo
     */
    public void changeObjetive( int node_id ){
        this.objetive_id = node_id;
    }
    /**
     * Actualiza el mapa
     * 
     * @param map Mapa nuevo
     */
    public void changeMap( int[][] map ){
        this.map = map;
    }
    /**
     * Calcula el coste del nodo actual hasta la posición del nodo inicial
     * 
     * @param node_id ID del nodo a saber su coste
     * @return 1 Si el nodo está en la cercanía de la posición inicial
     *         2 Si el nodo está más alejado
     */
    private int costToCurrentNode(int node_id) {
        boolean ext_node = node_id / 5 == 4 || node_id / 5 == 0
                || node_id % 5 == 4 || node_id % 5 == 0;
        return ext_node ? 2 : 1;
    }
    /**
     * Calcula el coste dle nodo actual hasta la posición del nodo objetivo
     * 
     * @param node_id ID del nodo a saber su coste
     * @return Valor del coste
     */
    private int costToObjetiveNode(int node_id) {
        int coord_i = node_id / 5;
        int coord_j = node_id % 5;
        int obj_i = this.objetive_id / 5;
        int obj_j = this.objetive_id % 5;

        return Math.abs(coord_i - obj_i) + Math.abs(coord_j - obj_j);
    }
    /**
     * Calcula el coste total de moverse a un Nodo dado
     * 
     * @param node_id ID del nodo a saber su coste
     * @return Coste total
     */
    private int calculateCostNode(int node_id) {
        return costToCurrentNode(node_id) + costToObjetiveNode(node_id);
    }
    /**
     * Comprueba si un Nodo se encuentra dentro de <open_list>
     * 
     * @param node_id ID del nodo a comprobar.
     * @return exist <true> si se encuentra, <false> en caso contrario
     */
    private boolean containsNodeOpenList(int node_id) {
        boolean exist = false;
        for (Node current_node : open_list) {
            if (current_node.getId() == node_id) {
                exist = true;
            }
        }
        return exist;
    }
    /**
     * Compreuba si un Nodo se encuentra dentor de <close_list>
     * 
     * @param node_id ID del nodo a comprobar
     * @return exist <true> si se encuentra, <false> en caso contrario
     */
    private boolean containsNodeCloseList(int node_id) {
        boolean exist = false;
        for (Node current_node : close_list) {
            if (current_node.getId() == node_id) {
                exist = true;
            }
        }
        return exist;
    }
    /**
     * Comprueba si un Nodo se encuentra en alguna lista <open_list> o <close_list>
     * 
     * @param node_id ID del nodo a comprobar
     * @return <true> si se encuentra en alguna, <false> en caso contrario
     */
    private boolean containsNodeSomeList(int node_id) {
        return containsNodeOpenList(node_id) || containsNodeCloseList(node_id);
    }
    /**
     * Añade a <open_list> todos los nodos adyacentes a uno dado siempre que no 
     * se encuetren ya dentro.
     * 
     * @param node_id ID del Nodo del que se quieren añadr todos sus adyacentes
     */
    private void addNextNodesOpenList(int node_id) {
        int[] dif_coord_i = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dif_coord_j = {-1, 0, 1, -1, 1, -1, 0, 1};
        // coordenadas del nodo actual
        int coord_i;
        int coord_j;
        int id_next_node;

        for (int i = 0; i < 8; i++) {
            id_next_node = (node_id / 5 + dif_coord_i[i]) * 5 + (node_id % 5 + dif_coord_j[i]);
            coord_i = node_id / 5 + dif_coord_i[i];
            coord_j = node_id % 5 + dif_coord_j[i];
            // no está en la matriz& id_next_node <= 24;
            // no es el raíz
            boolean is_in_matrix = coord_i >= 0 && coord_i <= 4 && coord_j >= 0 && coord_j <= 4;
            // no es el raíz
            boolean is_not_agent = id_next_node != start_id;
            // no está en ninguna lista
            boolean is_not_any_list = !containsNodeSomeList(id_next_node);
            // es alcanzable
            boolean is_reachable = false;
            if (is_in_matrix) {
                is_reachable = map[id_next_node / 5][id_next_node % 5] != -1;
            }
            // Si el nodo está en la matriz, no es el raiz, no se encuentra en open_list y es accesible
            if (is_not_agent && is_not_any_list && is_reachable) {
                open_list.add(new Node(id_next_node, node_id, calculateCostNode(id_next_node)));
            }
        }
    }

    /**
     * Obtiene el nodo con el menor coste de entre todos los que hay 
     * en <open_list>. Se presupone que la lista no está vacía
     * 
     * @return node Devuelve el nodo con el coste mejor
     */
    private Node getBetterNode() {
        int low_cost = (int) Math.pow(10, 10);
        Node node = null;
        for (Node current_node : open_list) {
            if (current_node.getCost() < low_cost) {
                node = current_node;
                low_cost = node.getCost();
            }
        }
        return node;
    }
    /**
     * Comprueba si el nodo objetivo se encuentra en <open_list>
     * 
     * @return <true> si <objetive_id> está en <open_list>
     */
    private boolean isObjetive() {
        return containsNodeOpenList(objetive_id);
    }
    /**
     * Obtiene un nodo dado de <open_list>. Se presupone que el nodo existe.
     * 
     * @param node_id ID del nodo que queremos obtener
     * @return node Nodo que queríamos obtener
     */
    private Node getNodeOpenList(int node_id) {
        Node node = null;
        for (Node current_node : open_list) {
            if (current_node.getId() == node_id) {
                node = current_node;
            }
        }
        return node;
    }
    /**
     * Obtiene un nodo dado de <close_list>. Se presupone que el nodo existe.
     * 
     * @param node_id ID del nodo que queremos obtener
     * @return node Nodo que queríamos obtener
     */
    private Node getNodeCloseList(int node_id) {
        Node node = null;
        for (Node current_node : close_list) {
            if (current_node.getId() == node_id) {
                node = current_node;
            }
        }
        return node;
    }
    /**
     * Calcula la ruta óptima entre la posición inicial <start_id> y la final 
     * <objetive_id>.
     * Guarda los ID´s de las casillas en <path_objetive>.
     * 
     */
    private void pathObjetive() {
        boolean encontrado = false;
        boolean agent = false;

        Node node = null;
        path_objetive.clear();
        open_list.clear();
        close_list.clear();

        close_list.add(new Node(start_id, -1, -1));
        addNextNodesOpenList(start_id);
        if(isObjetive()){
                    encontrado = true;
                }
        while (!encontrado) {
            node = getBetterNode();
            open_list.remove(node);
            close_list.add(node);
            addNextNodesOpenList(node.getId());
            encontrado = isObjetive();
        }

        path_objetive.add(objetive_id);
        node = getNodeOpenList(objetive_id);
        while (!agent) {
            node = getNodeCloseList(node.getParentId());
            path_objetive.add(0, node.getId());
            if (node.getId() == start_id) {
                agent = true;
            }
        }
    }
    /**
     * Devuelve el camino óptimo guardado
     * 
     * @return <path_objetive>
     */
    public ArrayList<Integer> getPath() {
        this.pathObjetive();
        return path_objetive;
    }
    /*public ArrayList<int[]> getPath(){
        ArrayList<int[]> path = new ArrayList<>();
        int[] coord = new int[2];
        for (int i = 0; i < path_objetive.size(); i++) {
            
            coord[0] = path_objetive.get(i)/5;
            coord[1] = path_objetive.get(i)%5;
            path.add(coord);
            System.out.println("GUARDANDO ---------------------------------------------> id: "+path_objetive.get(i)+ "coordenadas: " + coord[0] + ","+ coord[1]);
        }
        return path;
    }*/
    /**
     * Imprime por pantalla los ID´s de las casillas que forman el camino óptimo.
     */
    public void printPath() {
        for (int i = 0; i < path_objetive.size(); i++) {
            System.out.print(path_objetive.get(i) + "-");
        }
        System.out.println("\n");
    }
    /**
     * Imprime por pantalla:
     * <start_id>
     * <objetive_id>
     * <open_list>
     * <close_lsit>
     */
    private void printData() {
        System.out.println("Agent ID: " + start_id);
        System.out.println("Objetive ID: " + objetive_id);
        // Impresión del mapa
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                System.out.print(this.map[i][j] + " ");
            }
            System.out.println("\n");
        }

        // Impresión de close y open list
        System.out.println("\nOpen List: \n");
        for (Node current_node : open_list) {
            System.out.println("Nodo id: " + current_node.getId() + " Padre id: " + current_node.getParentId() + " Coste: " + calculateCostNode(current_node.getId()));
        }
        System.out.println("\nClose List: ");
        for (Node current_node : close_list) {
            System.out.println("Nodo id: " + current_node.getId() + " Padre id: " + current_node.getParentId() + " Coste: " + calculateCostNode(current_node.getId()));
        }
    }
}
