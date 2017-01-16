package practica3;


/**
 * Clase que contiene la información sobre el mundo que recibe de un vehiculo.
 * @author Luis Gallego Quero
 */
public class Percepcion {
    
    private Cell gps;
    private int bateria;
    private int[][] radar;
    private int energia;
    private boolean llegado;
    private String nombreVehiculo;
    
    public Cell getGps() {
	return this.gps;
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

    public int[][] getRadar() {
	return this.radar;
    }

    public void setRadar(int[][] radar) {
	this.radar = radar;
    }

    public int getEnergia() {
	return this.energia;
    }

    public void setEnergia(int energia) {
	this.energia = energia;
    }

    public boolean getLlegado() {
	return this.llegado;
    }

    public void setLlegado(boolean llegado) {
        this.llegado = llegado;
    }

    public String getNombreVehicle() {
	return this.nombreVehiculo;
    }

    public void setNombreVehicle(String nombreDrone) {
	this.nombreVehiculo = nombreDrone;
    }
}
