package practica3;

import java.awt.Point;

/**
 * Clase que contiene la información que tiene el controlador sobre un vehiculo.
 * @author Luis Gallego Quero
 */
public class PropiedadesVehicle {
    
    private Point gps;
    private int bateria;
    private boolean llegado;
    private int[][] radar;
    private Rol rol;
    
    public PropiedadesVehicle() {
	super();
    }
    public int[][] getRadar(){
        return this.radar;
    }
    public void setRadar(int[][] radar){
        this.radar = radar;
    }
    public int[] getGps() {
        int[] gps = new int[2];
        gps[0]= this.gps.x;
        gps[1]= this.gps.y;
	return gps;
    }

    public void setGps(Point gps) {
	this.gps = gps;
    }

    public int getBateria() {
	return this.bateria;
    }

    public void setBateria(int bateria) {
	this.bateria = bateria;
    }

    public boolean getLlegado() {
	return this.llegado;
    }

    public void setLlegado(boolean llegado) {
	this.llegado = llegado;
    }

    public Rol getRol() {
	return this.rol;
    }

    public void setRol(Rol rol) {
	this.rol = rol;
    }

    public void setRol(int consumo, int alcance, boolean volar) {
	this.rol = Rol.getRol(consumo,alcance,volar);
    }
    
    /**
     * Actualiza los parametros a partir de la percepción.
     * 
     * @param percepcion Percepción con los datos a actualizar.
     * @author Luis Gallego Quero
     */
    public void actualizarPercepcion(Percepcion percepcion){
        gps = percepcion.getGps();
        bateria = percepcion.getBateria();
        llegado = percepcion.getLlegado();
    }    
}
