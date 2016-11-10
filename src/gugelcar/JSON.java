/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gugelcar;
import com.eclipsesource.json.*;

/**
 *
 * @author luis
 */
public class JSON {
    
    // Caracteres para la comunicacion    
    private static String key; 
    
    /**
     * Creamos la primera conexión, para ello mandamos
     * un mapa concreto y los sensores que elegimos.
     * @author Luis Gallego
     * @author German Valdearenas
     * @return Devuelve un String con la cadena Json
     */
    public static String realizarLogin() {
        /* La idea sería que el main nos solicitara
        el mapa a ejecutar.
        */
        String mapa = "map1";
        JsonObject objeto = new JsonObject();
        
        objeto.add("command", "login");
        objeto.add("world", mapa);
        objeto.add("radar", "GugelCar");
        objeto.add("scanner", "GugelCar");
        objeto.add("gps", "GugelCar");
        
        return objeto.toString();        
    }
    
    /**
     * Comprobamos el login
     * @param respuesta Cadena que devuelve el servidor en Json
     * @author Luis Gallego
     * @author German Valdearenas
     * @return true si conecta, false en caso contrario
     */
    public static boolean resultadoLogin(String respuesta) {
        boolean resultado;
        
        if(respuesta.contains("BAD_")){
            resultado = false;
        } else {
            JsonObject objeto = Json.parse(respuesta).asObject();
            key = objeto.getString("result", null);
            System.out.println("Key obtenida " + key );
            if( key == null ) {
                resultado = false;
            } else {
                resultado = true;
            }
        }
        return resultado;        
    }
    
    /**
     * Ejecutamos las diferentes acciones posibles
     * @param accion Paso a realizar
     * @author Luis Gallego
     * @return Devuelve un String con la cadena Json
     */
    public static String realizarAccion(String accion) {
        /* La accion la pasamos desde el AgentCar
        para indicar el movimiento a realizar o el logout...
        */
        JsonObject objeto = new JsonObject();
        
        objeto.add("command", accion);
        objeto.add("key", key);
        
        return objeto.toString();
    }
    
    /**
     * Comprobamos el resultado de la accion
     * @param respuesta Cadena que devuelve el servidor en Json
     * @author Luis Gallego
     * @return true si llega correctamente, false en caso contrario
     */
    public static boolean resultadoAccion(String respuesta) {
        boolean resultado = respuesta.contains("OK");
        
        return resultado;
    }
    
}
