package practica3;

/**
 * @author Raúl López Arévalo
 * 
 * Clase que se comporta como un nodo. Cada nodo tiene un identificador ID, el 
 * identificador ID de su padre y lo costoso que sería desplazarse hacía él.
 */
public class Node {
    private int box_id;
    private int parent_id;
    private int cost;
    /**
     * Constructor
     * 
     * @param box_id ID del nodo 
     * @param parent_id ID de su padre
     * @param cost Como de costoso sería moverse al nodo
     */
    public Node( int box_id, int parent_id, int cost){
        this.box_id = box_id;
        this.parent_id = parent_id;
        this.cost = cost;
    }
    /**
     * Obtiene el ID del nodo
     * @return box_id
     */
    public int getId(){
        return box_id;
    }
    /**
     * Obtiene el ID del padre del nodo
     * @return parent_id
     */
    public int getParentId(){
        return parent_id;
    }
    /**
     * Obtiene el coste del nodo
     * @return cost
     */
    public int getCost(){
        return cost;
    }
    /**
     * Calcula la coordenada X que le correspondería en la matriz de mapa
     * @return Devuelve la coordenada X como int
     */
    public int getCoorX(){
        return box_id / 5;
    }
    /**
     * Calcula la coordenada Y que le correspondería en la matriz de mapa
     * @return Devuelve la coordenada Y como int
     */
    public int getCoorY(){
        return box_id % 5;
    }
    /**
     * Imprime por pantalla las propiedades del nodo:
     * box_id Su ID
     * parent_id El ID de su padre
     * cost Su coste
     */
    public void print(){
        System.out.println("ID: "+box_id+" Parent ID: "+parent_id+ " Cost: "+cost);
    }
}
