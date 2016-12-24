/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

/**
 *
 * @author Luis Gallego Quero
 */
public enum Rol {
    
    ROL1(0,2,0), ROL2(1,1,1), R0L3(2,4,2);   
    
    private int id;
    private int consumo;
    private int prioridad;
    
    private Rol(int idd, int cons, int prio) {
        id = idd;
        consumo = cons;
        prioridad = prio;
    }
    
    public int getId() {
	return id;
    }
    
    public int getConsumo() {
        return consumo;
    }    

    public int getPrioridad() {
	return prioridad;
    }
    
    public static Rol getRol(int id) {
        for(Rol x : Rol.values()){
            if(x.id == id) {
                return x;
            }
        }
        return null;
    }
    
}
