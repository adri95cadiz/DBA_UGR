package gugelcar;


/**
 * Contiene la clase Cell, una clase usada para encapsular el contenido de
 * una celda de la Matriz.
 * 
 * @author Samuel Peregrina Morillas
 * @version 1.0
 */
public class Cell {
    private int pos_x, pos_y, content;

    /**
     * Método constructor sin parámetros
     *
     */
    public Cell () {
        pos_x = -1;
        pos_y = -1;
        content = -1;
    }
    
    /**
     * Método constructor con todos los parámetros necesarios
     * 
     * @param pos_x Posición X de la celda
     * @param pos_y Posición Y de la celda
     * @param content Contenido de la celda
     */
    public Cell(int pos_x, int pos_y, int content ){
        this.pos_x = pos_x;
        this.pos_y = pos_y;
        this.content = content;
    }

    /**
     * Método para guardar el contenido de de una celda
     *
     * @param pos_x Posición X de la celda
     * @param pos_y Posición Y de la celda
     * @param content Contenido de la celda
     */
    public void set(int pos_x, int pos_y, int content){
        this.pos_x = pos_x;
        this.pos_y = pos_y;
        this.content = content;
    }

    /**
     * Método que devuelve la posición X de la celda
     *
     * @return Entero correspondiente a la posición X
     */
    public int getPosX(){
        return pos_x;
    }

    /**
     * Método que devuelve la posición Y de la celda
     *
     * @return Entero correspondiente a la posición Y
     */
    public int getPosY(){
        return pos_y;
    }
    
    /**
     * Método que devuelve la posición actual en formato array
     *
     * @return Un array de dos enteros correspondiente a la posición x e y
     */
    public int[] getPosition(){
        return new int[]{pos_x, pos_y};
    }

    /**
     * Método para comprobar si una celda ocupa una posición completa
     *
     * @param pos_x Posición X a comprobar
     * @param pos_y Posición Y a comprobar
     * @return Devuelve true en el caso de que si la ocupe, false en caso contrario
     */
    public boolean isPosition(int pos_x, int pos_y){
        return this.pos_x == pos_x && this.pos_y == pos_y;
    }

    /**
     * Método que devuelve el contenido de la celda
     *
     * @return Un entero correspondiente al contenido de la celda
     */
    public int getContains(){
        return content;
    }
}