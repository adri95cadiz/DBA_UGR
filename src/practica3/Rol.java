package practica3;

/**
 *
 * @author Luis Gallego Quero
 */
public enum Rol {
    
    AVION(0,2,3,true,0), COCHE(1,1,5,false,1), CAMION(2,4,11,false,2);   
    
    private int id;
    private int consumo;
    private int alcance;
    private boolean volar;
    private int prioridad;
    
    private Rol(int idd, int cons, int alc, boolean fly, int prio) {
        id = idd;
        consumo = cons;
        alcance = alc;
        volar = fly;
        prioridad = prio;
    }
    
    public int getId() {
	return id;
    }
    
    public int getConsumo() {
        return consumo;
    }    
    
    public int getAlcance() {
        return alcance;
    }
    
    public boolean getVolar() {
        return volar;
    }

    public int getPrioridad() {
	return prioridad;
    }
    
    /**
     * Obtiene el rol que contiene un id concreto.
     * @param id Identificación del rol.
     * @author Luis Gallego Quero
     * @return El rol identificado.
     */
    public static Rol getRol(int id) {
        for(Rol x : Rol.values()){
            if(x.id == id) {
                return x;
            }
        }
        return null;
    }    
}
