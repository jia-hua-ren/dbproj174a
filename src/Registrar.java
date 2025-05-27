import java.sql.*;
import java.io.*;
import java.util.*;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;

public class Registrar {
    static String DB_URL = "";
    final static String DB_USER = "ADMIN";
    static String DB_PASSWORD = "";

    static Scanner scanner = new Scanner(System.in);
    static OracleConnection connection;

    public static void main(String[] args) throws SQLException {
        loadEnv();

        Properties info = new Properties();
        info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, DB_USER);
        info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, DB_PASSWORD);
        info.put(OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH, "20");

        OracleDataSource ods = new OracleDataSource();
        ods.setURL(DB_URL);
        ods.setConnectionProperties(info);

        try (OracleConnection conn = (OracleConnection) ods.getConnection()) {
            connection = conn;
            System.out.println("Connected to the database!");

            boolean running = true;
            while (running) {
                printMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        addStudentToCourse();
                        break;
                    case "2":
                        dropStudentFromCourse();
                        break;
                    case "3":
                        listCoursesForStudent();
                        break;
                    case "4":
                        listGradesForStudent();
                        break;
                    case "5":
                        generateClassList();
                        break;
                    case "6":
                        enterGradesFromFile();
                        break;
                    case "7":
                        requestTranscript();
                        break;
                    case "8":
                        generateGradeMailer();
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option. Try again.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void loadEnv() {
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
    }

    private static void printMenu() {
        System.out.println("\n===== Registrar CLI Menu =====");
        System.out.println("1. Add student to a course");
        System.out.println("2. Drop student from a course");
        System.out.println("3. List courses taken by a student");
        System.out.println("4. List grades for previous quarter");
        System.out.println("5. Generate class list for a course");
        System.out.println("6. Enter grades from file");
        System.out.println("7. Request transcript");
        System.out.println("8. Generate grade mailer for all students");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void addStudentToCourse() {
        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine().trim();
        System.out.print("Enter course ID: ");
        String courseId = scanner.nextLine().trim();

        String sql = "INSERT INTO is_taking (_, _, _, _) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Student added successfully.");
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }


        // TODO: Implement DB insert logic
        System.out.println("Student " + studentId + " added to course " + courseId);
    }

    private static void dropStudentFromCourse() {
        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine().trim();
        System.out.print("Enter course ID: ");
        String courseId = scanner.nextLine().trim();
        // TODO: Implement DB delete logic
        System.out.println("Student " + studentId + " dropped from course " + courseId);
    }

    private static void listCoursesForStudent() {
        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine().trim();
        // TODO: Implement DB query logic
        System.out.println("Courses for student " + studentId + ":");
    }

    private static void listGradesForStudent() {
        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine().trim();
        // TODO: Implement DB query for grades in previous quarter
        System.out.println("Grades for student " + studentId + " (previous quarter):");
    }

    private static void generateClassList() {
        System.out.print("Enter course ID: ");
        String courseId = scanner.nextLine().trim();
        // TODO: Implement DB query logic
        System.out.println("Class list for course " + courseId + ":");
    }

    private static void enterGradesFromFile() {
        System.out.print("Enter filename: ");
        String filename = scanner.nextLine().trim();

        try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                String[] parts = line.split(",");
                String studentId = parts[0].trim();
                String grade = parts[1].trim();
                // TODO: Insert/update grade in DB
                System.out.println("Processed grade for " + studentId + ": " + grade);
            }
        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
        }
    }

    private static void requestTranscript() {
        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine().trim();
        // TODO: Query grades and course history
        System.out.println("Transcript for student " + studentId + ":");
    }

    private static void generateGradeMailer() {
        // TODO: Fetch all students and generate mailer
        System.out.println("Grade mailer generated for all students.");
    }
}
