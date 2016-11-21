/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gugelcar;

import java.util.ArrayList;

/**
 *
 * @author Lopeare
 */
public class Path {

    private ArrayList<Integer> path_objetive = new ArrayList<>();
    private ArrayList<Node> open_list = new ArrayList<>();
    private ArrayList<Node> close_list = new ArrayList<>();
    private int objetive_id;
    private int agent_id;
    private int[][] map;
    
    public Path(int[][] map, int agent_id, int objetive_id) {
        this.map = map;
        this.map[objetive_id / 5][objetive_id % 5] = 2;
        this.agent_id = agent_id;
        this.objetive_id = objetive_id;
    }
    public void changeObjetive( int node_id ){
        this.objetive_id = node_id;
    }
    public void changeMap( int[][] map ){
        this.map = map;
    }
    // Devuelve el coste del nodo actual hasta la posicion del agente
    public int costToCurrentNode(int node_id) {
        boolean ext_node = node_id / 5 == 4 || node_id / 5 == 0
                || node_id % 5 == 4 || node_id % 5 == 0;
        return ext_node ? 2 : 1;
    }

    public int costToObjetiveNode(int node_id) {
        int coord_i = node_id / 5;
        int coord_j = node_id % 5;
        int obj_i = this.objetive_id / 5;
        int obj_j = this.objetive_id % 5;

        return Math.abs(coord_i - obj_i) + Math.abs(coord_j - obj_j);
    }

    public int calculateCostNode(int node_id) {
        return costToCurrentNode(node_id) + costToObjetiveNode(node_id);
    }

    // Comprueba si un nodo dado está en open_list
    public boolean containsNodeOpenList(int node_id) {
        boolean exist = false;
        for (Node current_node : open_list) {
            if (current_node.getId() == node_id) {
                exist = true;
            }
        }
        return exist;
    }

    // Comprueba si un nodo dado está en close_list
    public boolean containsNodeCloseList(int node_id) {
        boolean exist = false;
        for (Node current_node : close_list) {
            if (current_node.getId() == node_id) {
                exist = true;
            }
        }
        return exist;
    }

    // Comprueba si un nodo está en alguna lista
    public boolean containsNodeSomeList(int node_id) {
        return containsNodeOpenList(node_id) || containsNodeCloseList(node_id);
    }

    // Añade a open_list los nodos adyacentes a uno dado
    public void addNextNodesOpenList(int node_id) {
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
            boolean is_not_agent = id_next_node != agent_id;
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

    // Toma el nodo con un coste menor y lo devuelve
    public Node getBetterNode() {
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

    public boolean isObjetive() {
        return containsNodeOpenList(objetive_id);
    }

    // Recupera un nodo dado su id de open_list
    public Node getNodeOpenList(int node_id) {
        Node node = null;
        for (Node current_node : open_list) {
            if (current_node.getId() == node_id) {
                node = current_node;
            }
        }
        return node;
    }

    // Recupera un nodo padre dado el id de su hijo de close_list
    public Node getNodeCloseList(int node_id) {
        Node node = null;
        for (Node current_node : close_list) {
            if (current_node.getId() == node_id) {
                node = current_node;
            }
        }
        return node;
    }

    public void pathObjetive() {
        boolean encontrado = false;
        boolean agent = false;

        Node node = null;
        path_objetive.clear();
        open_list.clear();
        close_list.clear();

        close_list.add(new Node(agent_id, -1, -1));
        addNextNodesOpenList(agent_id);
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
            if (node.getId() == agent_id) {
                agent = true;
            }
        }
    }

    public ArrayList<Integer> getPath() {
        ArrayList<Integer> path = new ArrayList<>();
        for (int i = 0; i < path_objetive.size(); i++) {
            path.add(path_objetive.get(i));
        }
        return path;
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

    public void printPath() {
        for (int i = 0; i < path_objetive.size(); i++) {
            System.out.print(path_objetive.get(i) + "-");
        }
        System.out.println("\n");
    }

    public void printData() {
        System.out.println("Agent ID: " + agent_id);
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
