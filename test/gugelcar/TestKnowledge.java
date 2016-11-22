/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gugelcar;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 *
 * @author gaedr
 */
public class TestKnowledge {
    public static void main(String[] args) throws Exception {
        Knowledge bd = Knowledge.getDB(10);
        bd.drawMap();
        /*int turn = 5;
        int[][] mapa;
        
        // Setting GPS
        JsonObject gps = new JsonObject();
        JsonObject coord = new JsonObject();
        coord.add("x", 25);
        coord.add("y", 20);
        gps.add("gps", coord);
        
        
        // Setting radar
        JsonObject radar = new JsonObject();
        JsonArray radarValue = new JsonArray();
        int[] matrixValue = new int[]{
            0,0,0,0,0,
            0,1,0,0,0,
            0,1,0,0,0,
            0,1,1,0,0,
            0,0,0,0,0
        };
        for(int cell : matrixValue){
            radarValue.add(cell);
        }
        radar.add("radar", radarValue);

        // Printing values
        System.out.println(radar);
        System.out.println(gps);
        System.out.println(turn);
        
        // Test
        mapa = bd.updateStatus(radar, gps, turn);
        
        // Printing
        bd.drawMap();
        
        
        // Setting GPS
        gps = new JsonObject();
        coord = new JsonObject();
        coord.add("x", 24);
        coord.add("y", 20);
        gps.add("gps", coord);
        
        
        // Setting radar
        radar = new JsonObject();
        radarValue = new JsonArray();
        matrixValue = new int[]{
            0,1,1,1,1,
            0,0,0,0,0,
            0,1,0,0,0,
            0,1,0,0,0,
            0,1,1,0,0
        };
        for(int cell : matrixValue){
            radarValue.add(cell);
        }
        radar.add("radar", radarValue);
        
        System.out.println(radar);
        System.out.println(gps);
        System.out.println(turn);
        
        mapa = bd.updateStatus(radar, gps, 6);
       bd.drawMap();
       
       
       
       
       
       
       
       // Setting GPS
        gps = new JsonObject();
        coord = new JsonObject();
        coord.add("x", 23);
        coord.add("y", 20);
        gps.add("gps", coord);
        
        
        // Setting radar
        radar = new JsonObject();
        radarValue = new JsonArray();
        matrixValue = new int[]{
            0,1,1,0,0,
            0,1,1,1,1,
            0,0,0,0,0,
            0,1,0,0,0,
            0,1,0,0,0
           
        };
        for(int cell : matrixValue){
            radarValue.add(cell);
        }
        radar.add("radar", radarValue);
        
        System.out.println(radar);
        System.out.println(gps);
        System.out.println(turn);
        
        mapa = bd.updateStatus(radar, gps, 7);
       bd.drawMap();*/
    }
}
