package gugelcar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database
 */
public class Database {
    private Database instance = null;
    private int map_id;

    public Database getDB(int map_id){
        if(instance == null){
            instance = new Database();
        }
        this.map_id = map_id;
        return instance;
    }

    private Database() {
        Class.forName("org.sqlite.JDBC");

        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:mapas.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);

            String creteTableSQL = "CREATE TABLE IF NOT EXIST Mapa_" + this.map_id + "(
                pos_x INTEGER NOT NULL,
                pos_y INTEGER NOT NULL,
                state INTEGER DEFAULT 0,
                value INTEGER DEFAULT 0,
                CONSTRAINT pk_posicion PRIMARY KEY (pos_x, pos_y)
            )";

            statement.executeUpdate(creteTableSQL);
        } catch (SQLException e) {
            //TODO: handle exception
        }
    }
}