package practica3;

/**
 *
 * @author Luis Gallego Quero
 */
public enum Rol {
    
    AVION(0,2,3,true), COCHE(1,1,5,false), CAMION(2,4,11,false);   
    
    private int id;
    private int consumo;
    private int alcance;
    private boolean volar;
    
    private Rol(int idd, int cons, int alc, boolean fly) {
        id = idd;
        consumo = cons;
        alcance = alc;
        volar = fly;
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
    
    /**
     * Obtiene el rol que contiene un id concreto.
     * @param id Identificación del rol.
     * @author Luis Gallego Quero
     * @return El rol identificado.
     */
    public static Rol getRol(int consumo, int alcance, boolean volar) {
       
        if(consumo==2 && alcance==3 && !volar) 
            return AVION;
        else if(consumo==1 && alcance==5 && !volar) 
            return COCHE;
        else
            return CAMION;
        //return null;
    }    
}
