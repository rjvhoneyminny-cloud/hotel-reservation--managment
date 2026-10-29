package hotel;

import java.sql.*;

public final class Db {
    private Db() {}
    public static Connection connect() throws SQLException {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/hotel_reservation?serverTimezone=UTC");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
    public static void initialize() throws SQLException {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("INSERT IGNORE INTO rooms(room_number,room_type,capacity,nightly_rate) VALUES ('101','Single',1,65.00),('202','Double',2,95.00),('303','Suite',4,180.00)");
        }
    }
}
