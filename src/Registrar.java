import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;


import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import java.sql.DatabaseMetaData;

public class Registrar {
    static String DB_URL = "";
    final static String DB_USER = "ADMIN";
    static String DB_PASSWORD = "";

    
    // This method creates a database connection using
    // oracle.jdbc.pool.OracleDataSource.
    public static void main(String args[]) throws SQLException {
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("DB_URL=")) {
                    DB_URL = line.substring("DB_URL=".length()).trim();
                } else if (line.startsWith("DB_PASSWORD=")) {
                    DB_PASSWORD = line.substring("DB_PASSWORD=".length()).trim();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading .env file: " + e.getMessage());
        }

        Properties info = new Properties();
            System.out.println("DB_URL: " + DB_URL);

        System.out.println("Initializing connection properties...");
        info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, DB_USER);
        info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, DB_PASSWORD);
        info.put(OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH, "20");

        System.out.println("Creating OracleDataSource...");
        OracleDataSource ods = new OracleDataSource();

        System.out.println("Setting connection properties...");
        ods.setURL(DB_URL);
        ods.setConnectionProperties(info);

        // With AutoCloseable, the connection is closed automatically
        try (OracleConnection connection = (OracleConnection) ods.getConnection()) {
            System.out.println("Connection established!");
            // Get JDBC driver name and version
            DatabaseMetaData dbmd = connection.getMetaData();
            System.out.println("Driver Name: " + dbmd.getDriverName());
            System.out.println("Driver Version: " + dbmd.getDriverVersion());
            // Print some connection properties
            System.out.println(
                "Default Row Prefetch Value: " + connection.getDefaultRowPrefetch()
            );
            System.out.println("Database username: " + connection.getUserName());
            System.out.println();
            
        } catch (Exception e) {
            System.out.println("CONNECTION ERROR:");
            System.out.println(e);
        }
    }
}
