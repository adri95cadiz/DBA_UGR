/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gugelcar;

/**
 *
 * @author Lopeare
 */
public class Node {
    private int box_id;
    private int parent_id;
    private int cost;
    
    public Node( int box_id, int parent_id, int cost){
        this.box_id = box_id;
        this.parent_id = parent_id;
        this.cost = cost;
    }
    
    public int getId(){
        return box_id;
    }
    
    public int getParentId(){
        return parent_id;
    }
    
    public int getCost(){
        return cost;
    }
    
    public int getCoorX(){
        return box_id / 5;
    }
    
    public int getCoorY(){
        return box_id % 5;
    }
    
    public void print(){
        System.out.println("ID: "+box_id+" Parent ID: "+parent_id+ " Cost: "+cost);
    }
}
