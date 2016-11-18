/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gugelcar;
import com.eclipsesource.json.*;
import java.util.ArrayList;

/**
 *
 * @author Luis Gallego
 * @author German Valdearenas
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
        /* La idea sera que el main nos solicitara
        el mapa a ejecutar.
        */
        String mapa = "map1";
        JsonObject objeto = new JsonObject();
        
        objeto.add("command", "login");
        objeto.add("world", mapa);
        objeto.add("radar", "GugelCar");
        objeto.add("scanner", "GugelCar");
        objeto.add("gps", "GugelCar");
        
        //System.out.println("Peticion de login realizada");
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
        System.out.println("Entramos en resultado login.");
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
    /**
     * La informacion del Scanner en Json la pasamos a una matriz
     * @param respuesta Cadena Json
     * @author Luis Gallego
     * @return Matriz de float con el contenido del Json
     */
    public static float[][] leerScanner(String respuesta) {
        float[][] scanner = new float[5][5];
        if(!respuesta.contains("CRASHED")) {
            JsonObject objeto = Json.parse(respuesta).asObject();
            JsonArray vector = objeto.get("scanner").asArray();
            int i=0, j=0;
            for(JsonValue valor : vector) {
                scanner[i][j] = valor.asFloat();
                j++;
                if(j==5) {
                    j=0;
                    i++;
                }
            }
        }
        return scanner;
    }
    
    /**
     * La informacion del Radar en Json la pasamos a una matriz
     * @param respuesta Cadena Json
     * @author Luis Gallego
     * @return Matriz de int con el contenido del Json
     */
    public static int[][] leerRadar(String respuesta) {
        int radar[][] = new int[5][5];
        if(!respuesta.contains("CRASHED")) {
            JsonObject objeto = Json.parse(respuesta).asObject();
            JsonArray vector = objeto.get("radar").asArray();
            int i=0, j=0;
            for(JsonValue valor : vector) {
                radar[i][j] = valor.asInt();
                j++;
                if(j==5) {
                    j=0;
                    i++;
                }
            }
        }
        return radar;
    }
    
    /**
     * La informacion del GPS en Json la pasamos a un vector
     * donde la posicion 0 es la x y la posicion 1 es la y
     * @param respuesta Cadena Json
     * @author Luis Gallego
     * @return Vector de int con el contenido del Json
     */
    public static int[] leerGPS(String respuesta) {
        int[] gps = new int[2];
        if(!respuesta.contains("CRASHED")) {
            JsonObject objeto = Json.parse(respuesta).asObject();
            JsonObject gpsObjeto = objeto.get("gps").asObject();
            
            System.out.println("el gps contiene ñalksjdflñjasdlkfjslñjfslñjkflñsjflñjksadlñfj : " + gpsObjeto);
            
            gps[0] = (gpsObjeto.get("y")).asInt();
            gps[1] = (gpsObjeto.get("x")).asInt();
        }
        return gps;
    }
    
    public static ArrayList<Integer> leerTraza(String respuesta) {
        ArrayList<Integer> traza = new ArrayList<>();
        if(!respuesta.contains("CRASHED")) {
            JsonObject objeto = Json.parse(respuesta).asObject();
            JsonArray vector = objeto.get("trace").asArray();
            for(JsonValue valor : vector) {
                traza.add(valor.asInt());
            }
        }
        return traza;
    }
}
