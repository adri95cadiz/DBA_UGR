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
     * @return 
     */
    public static String realizarLogin() {
        /* La idea sería que el main nos solicitara
        el mapa a ejecutar.
        */
        String mapa = "map2";
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
     * @return 
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
    
}
