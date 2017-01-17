package practica3;

import com.eclipsesource.json.*;
import java.awt.Point;

/**
 *
 * @author Luis Gallego
 */
public class JSON {
      
    /**
     * Generamos la cadena necesaria para registrarse en un mundo.
     * @param cadena Nombre del mapa.
     * @author Luis Gallego Quero
     * @return Cadena con el Json codificado.
     */
    public static String suscribirse(String cadena) {
        JsonObject objeto = new JsonObject();
        objeto.add("world", cadena);
        return objeto.toString();
    }
    
    /**
     * Almacena la key proporcionada por el controlador del servidor.
     * @param cadena Contenido de la key en Json.
     * @author Luis Gallego Quero
     */
    public static void leerKey(String cadena) {
        JsonObject objeto = Json.parse(cadena).asObject();
        String key = objeto.getString("result", null);
        //System.out.println("Key obtenida: " + key);
        //return key;
    }
       
    /**
     * Generamos la cadena necesaria para hacer login en el mundo.
     * @author Luis Gallego Quero
     * @return Cadena con el Json codificado.
     */
    public static String registrarse() { // checkin
        JsonObject objeto = new JsonObject();
        objeto.add("command", "checkin");
        return objeto.toString();
    }
    
    /**
     * Generamos la cadena necesaria para hacer un movimiento.
     * @param cadena Movimiento que se quiere realizar.
     * @author Luis Gallego Quero
     * @return Cadena con el Json codificado.
     */
    public static String mover(String cadena) {
        JsonObject objeto = new JsonObject();
        objeto.add("command", cadena);
        return objeto.toString();
    }
    
    /**
     * Genera la cadena necesaria para repostar.
     * @author Luis Gallego Quero
     * @return Cadena con el Json codificado.
     */
    public static String repostar() {
        JsonObject objeto = new JsonObject();
        objeto.add("command", "refuel");
        return objeto.toString();
    }
        
    /**
     * Convierte una cadena codificada en Json a un objeto JsonObject
     * @param cadena string codificado en Json
     * @author Luis Gallego Quero
     * @return objeto JsonObject creado a partir de la cadena
     */
    private static JsonObject parseToJson(String cadena) {
        return Json.parse(cadena).asObject();
    }
    
    /**
     * Cambia de JsonArray a matriz.
     * @param vector JsonArray con la info del radar.
     * @author Luis Gallego Quero
     * @return Matriz de enteros correspondiente al radar.
     */
    public static int[][] parseSensor(JsonArray vector) {
        final int TAM = (int) Math.sqrt(vector.size());
        int[][] matriz = new int[TAM][TAM];
        int i=0, j=0;
        for(JsonValue valor : vector){
            matriz[i][j] = valor.asInt();
            j++;
            if(j == TAM) {
                j=0;
                i++;
            }
        }
        return matriz;
    }
    
    private static int[] parseRadar(JsonArray vector){
        final int TAM = (int) (vector.size());
        int[] matriz = new int[TAM];
        int i=0, j=0;
        for(JsonValue valor : vector){
            matriz[i] = valor.asInt();
            i++;
        }
        return matriz;
    }
    
    
    /**
     * Convierte una cadena Json en un objeto Percepcion
     * @param cadena Contiene codificada las percepciones.
     * @author Luis Gallego Quero
     * @return Devuelve las percepciones de la cadena.
     */
    public static Percepcion getPercepcion(String cadena) {
        JsonObject objeto = parseToJson(cadena);
        JsonObject resultado = objeto.get("result").asObject();
        
        // Nivel de bateria
        Percepcion percepcion = new Percepcion();
        percepcion.setBateria(resultado.getInt("battery", -1));
        
        //Posicion en el mundo
        Cell gps = new Cell();
        gps.set(resultado.getInt("x", -1), resultado.getInt("y", -1),  0);
        percepcion.setGps(gps);
        
        // El radar lo recibimos como array pero lo utilizamos como matriz.
        int[][] radar = parseSensor(resultado.get("sensor").asArray());
        percepcion.setRadar(radar);
        
        // Nivel de energia global
        percepcion.setEnergia(resultado.getInt("energy", -1));
        
        // Ha llegado al objetivo?
        percepcion.setLlegado((resultado.getBoolean("goal", true)));
        //percepcion.setLlegado(radar[radar.length/2][radar.length/2] == 3);
        
        return percepcion;        
    }  
    
    /**
     * Convierte el rol codificado en Json a un valor entero.
     * @param cadena string que contiene el rol de un dron
     * @author Luis Gallego Quero
     * @return Devuelve el rol concreto de un dron
     */
    public static Rol getRol(String cadena) {
        int consumo;
        int alcance;
        boolean volar;
        
        JsonObject objeto = parseToJson(cadena);
        JsonObject resultado = objeto.get("capabilities").asObject();
        consumo = resultado.getInt("fuelrate", -1);
        alcance = resultado.getInt("range", -1);
        volar = resultado.getBoolean("fly", false);
        
        return Rol.getRol(consumo, alcance, volar);
    }
}
