package practica3;

/**
 *
 * @author Luis Gallego Quero
 */
public enum Celda {
    
    LIBRE, OBSTACULO, PARED, OBJETIVO, DESCONOCIDA, 
    RECORRIDO0, RECORRIDO1, RECORRIDO2, RECORRIDO3,
    ULT_POSICION0, ULT_POSICION1, ULT_POSICION2, ULT_POSICION3;
    
    /**
     * Valor de la celda para un vehículo concreto.
     * @param nombre Nombre del vehículo.
     * @author Luis Gallego Quero
     * @return Valor de la celda.
     */
    public static Celda getRecorrido(String nombre) {
	if (nombre.equals("Vehiculo10")) {
	    return RECORRIDO0;
	} else if (nombre.equals("Vehiculo11")) {
	    return RECORRIDO1;
	} else if (nombre.equals("Vehiculo12")) {
	    return RECORRIDO2;
	} else {
	    return RECORRIDO3;
	}
    }
    
    /**
     * Valor de la última posición para un vehículo concreto.
     * @param nombre Nombre del vehículo.
     * @author Luis Gallego Quero
     * @return Valor de la última posición.
     */
    public static Celda getUlt_Posicion(String nombre) {
	if (nombre.equals("Vehiculo0")) {
	    return ULT_POSICION0;
	} else if (nombre.equals("Vehiculo1")) {
	    return ULT_POSICION1;
	} else if (nombre.equals("Vehiculo2")) {
	    return ULT_POSICION2;
	} else {
	    return ULT_POSICION3;
	}
    }
    
    /**
     * Según el id del radar obtenemos la celda.
     * @param id Identificación en el radar ( 0,1,2 ó 3 ).
     * @author Luis Gallego Quero.
     * @return El valor de la celda.
     */
    public static Celda getCelda(int id) {
	Celda celda = DESCONOCIDA;
	switch (id) {
	    case 0:
		celda = LIBRE;
		break;
	    case 1:
		celda = OBSTACULO;
		break;
	    case 2:
		celda = PARED;
		break;
	    case 3:
		celda = OBJETIVO;
		break;
	}
	return celda;
    }    
}
