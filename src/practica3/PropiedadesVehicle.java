package practica3;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.ArrayList;

/**
 * Clase que contiene la información que tiene el controlador sobre un vehiculo.
 * @author Luis Gallego Quero
 */
public class PropiedadesVehicle {
    
    private Cell gps;
    private int [] posInicial = new int [2];
    private int bateria;
    private boolean llegado;
    private int[][] radar;
    private Rol rol;
    private int pasos;
    private String nombre;
    private GugelVehicleMatrix matriz;
    
    
    public PropiedadesVehicle() {
	super();
    }
    /**
     * @author Raúl López Arévalo
     */
    public int[][] getRadar(){
        return this.radar;
    }
    /**
     * @author Raúl López Arévalo
     */
    public void printRadar(){
        for(int i=0; i<radar.length; i++){
            for(int j=0; j<radar.length; j++){
                System.out.print(radar[i][j] + " ");
            }
            System.out.println("");
        }
    }
    /**
     * @author Raúl López Arévalo
     */
    public void printGps(){
        System.out.println("X: "+gps.getPosX()+" Y: "+gps.getPosY());
    }
    public void setRadar(int[][] radar){
        this.radar = radar;
    }
    
    public int[] getGps() {
        int[] gps = new int[2];
        gps[0]= this.gps.getPosX();
        gps[1]= this.gps.getPosY();
	return gps;
    }
    
    public void darPaso(){
        pasos++;
    }
    
    public int getPasos(){
        return pasos;
    }
    
    public GugelVehicleMatrix getMatrix(){
        return matriz;
    }
    
    public void setMatrix(GugelVehicleMatrix matriz){
        this.matriz = matriz;
    }
    
    public void updateMatrix(){
        matriz.updateMatrix(radar, gps);
    }
    
    public void setGps(Cell gps) {
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
    
    public String getNombre(){
        return nombre;
    }
    
    /**
     * Actualiza los parametros a partir de la percepción.
     * 
     * @param percepcion Percepción con los datos a actualizar.
     * @author Luis Gallego Quero
     */
    public void actualizarPercepcion(Percepcion percepcion){
        nombre = percepcion.getNombreVehicle();
        gps = percepcion.getGps();
        bateria = percepcion.getBateria();
        llegado = percepcion.getLlegado();
        radar = percepcion.getRadar();
    }    
    /**
     * @author Raúl López Arévalo
     */
    public void setPosInicial(Cell posInicial) {
        this.posInicial[0] = posInicial.getPosX();
        this.posInicial[1] = posInicial.getPosY();
    }
}
