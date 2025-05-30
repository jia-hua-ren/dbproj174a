import java.sql.*;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;

public class insertStudent {

    static String DB_URL = "";
    final static String DB_USER = "ADMIN";
    static String DB_PASSWORD = "";
    static OracleConnection connection;
    static Scanner scanner = new Scanner(System.in);

    public static void connectToDatabase() throws SQLException {
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("DB_URL=")) {
                    DB_URL = line.substring("DB_URL=".length()).trim();
                } else if (line.startsWith("DB_PASSWORD=")) {
                    DB_PASSWORD = line.substring("DB_PASSWORD=".length()).trim();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
        }

        Properties info = new Properties();
        info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, DB_USER);
        info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, DB_PASSWORD);
        info.put(OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH, "20");

        OracleDataSource ods = new OracleDataSource();
        ods.setURL(DB_URL);
        ods.setConnectionProperties(info);

        connection = (OracleConnection) ods.getConnection();
        System.out.println("Connected to the database!");

        return;
    }

    public static void main(String[] args) throws Exception {
        // Connect to the database
        connectToDatabase();
        bulkInsertStudents();
        // Student has perm, name, address, dept, pin
        System.out.println("Enter Student ID:");
        int studentId = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        System.out.println("Enter Student Name:");
        String studentName = scanner.nextLine();
        System.out.println("Enter Student Address:");
        String studentAddress = scanner.nextLine();
        System.out.println("Enter Student Department:");
        String studentDepartment = scanner.nextLine();
        System.out.println("Enter Student PIN:");
        String studentPin = scanner.nextLine();

        // Hash the pin value and store the hashed value
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(String.valueOf(studentPin).getBytes("UTF-8"));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        String hashedPin = hexString.toString();
        System.out.println("Hashed PIN: " + hashedPin.length());
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO students (perm, sname, address, mname, pin) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, studentId);
            preparedStatement.setString(2, studentName);
            preparedStatement.setString(3, studentAddress);
            preparedStatement.setString(4, studentDepartment);
            preparedStatement.setString(5, hashedPin);

            // Now insert the data into the database
            preparedStatement.executeUpdate();
            System.out.println("Student inserted successfully!");
        } catch (SQLException e) {
            System.out.println("Error inserting student:");
            e.printStackTrace();
        }
    }

    public static void bulkInsertStudents() throws Exception {
        // List of students: perm, name, address, department, pin
        String[][] students = {
                { "12345", "Alfred Hitchcock", "6667 El Colegio #40", "CS", "12345" },
                { "14682", "Billy Clinton", "5777 Hollister", "ECE", "14682" },
                { "37642", "Cindy Laugher", "7000 Hollister", "CS", "37642" },
                { "85821", "David Copperfill", "1357 State St", "CS", "85821" },
                { "38567", "Elizabeth Sailor", "4321 State St", "ECE", "38567" },
                { "81934", "Fatal Castro", "3756 La Cumbre Plaza", "CS", "81934" },
                { "98246", "George Brush", "5346 Foothill Av", "CS", "98246" },
                { "35328", "Hurvyson Ford", "678 State St", "ECE", "35328" },
                { "84713", "Ivan Lendme", "1235 Johnson Dr", "ECE", "84713" },
                { "36912", "Joe Pepsi", "3210 State St", "CS", "36912" },
                { "46590", "Kelvin Coster", "Santa Cruz #3579", "ECE", "46590" },
                { "91734", "Li Kung", "2 People's Rd Beijing", "ECE", "91734" },
                { "73521", "Magic Jordon", "3852 Court Rd", "CS", "73521" },
                { "53540", "Nam-hoi Chung", "1997 People's St HK", "CS", "53540" },
                { "82452", "Olive Stoner", "6689 El Colegio #151", "ECE", "82452" },
                { "18221", "Pit Wilson", "911 State St", "ECE", "18221" }
        };

        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO students (perm, sname, address, mname, pin) VALUES (?, ?, ?, ?, ?)");

    }

}
