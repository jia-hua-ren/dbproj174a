import java.sql.*;
import java.util.*;
import java.io.*;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;

public class goldInterface {
    static String DB_URL = "";
    final static String DB_USER = "ADMIN";
    static String DB_PASSWORD = "";
    static OracleConnection connection;

    // Connect to the database
    public static void main(String[] args) {
        // Create a connection to the database
        try {
            connectToDatabase();
        } catch (SQLException e) {
            System.out.println("Error connecting to the database:");
            System.out.println(e);
            return; // Exit if connection fails
        }
        // Find out what action the user wants to take
        System.out.println("Welcome to the Gold Interface!");
        System.out.println("Please choose an action:");
        System.out.println("1. Add a Course");
        System.out.println("2. Drop a Course");
        System.out.println("3. List Enrolled Courses (Current Quarter)");
        System.out.println("4. View Grades from Previous Quarter");
        System.out.println("5. Requirements Check");
        System.out.println("6. Make a Plan");
        System.out.println("7. Change PIN");
        System.out.println("8. Exit");

        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();

        switch (choice) {
            case 1:
                addCourse();
                break;
            case 2:
                dropCourse();
                break;
            case 3:
                listCurrentQuarterCourses();
                break;
            case 4:
                viewPreviousQuarterGrades();
                break;
            case 5:
                checkRequirements();
                break;
            case 6:
                makeStudyPlan();
                break;
            case 7:
                changePin();
                break;
            case 8:
                System.out.println("Exiting the application.");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }

    }

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

    public static void login(Connection connection) {
        // Implement login logic here
        // For example, you can use a Scanner to read user input for username and
        // password
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        // First validate that the username works
        while (true) {
            // Test the username
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT * FROM users WHERE username = ?");
                preparedStatement.setString(1, username);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (!resultSet.next()) {
                    System.out.println("Username does not exist, try again.");
                    username = scanner.nextLine();
                } else {
                    break;
                }
            } catch (SQLException e) {
                System.out.println("SQL ERROR:");
                System.out.println(e);
            }
        }

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        // Check that the username and password match valid entries in the database
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                System.out.println("Login successful!");
                // Proceed with the application logic
            } else {
                System.out.println("Wrong password, try again.");
                password = scanner.nextLine();
                preparedStatement.setString(2, password);
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    System.out.println("Login successful!");
                    // Proceed with the application logic
                } else {
                    System.out.println("Invalid username or password.");
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL ERROR:");
            System.out.println(e);
        }

    }

    public static void addCourse() {
        // First read in the perm number of the user
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your perm number: ");
        String permNumber = scanner.nextLine();
        // Validate the perm number
        while (true) {
            try {
                System.out.println("Validating perm number...");
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT * FROM students WHERE perm = ?");
                preparedStatement.setString(1, permNumber);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (!resultSet.next()) {
                    System.out.println("Perm number does not exist, try again.");
                    permNumber = scanner.nextLine();
                } else {
                    break;
                }
            } catch (SQLException e) {
                System.out.println("SQL ERROR:");
                System.out.println(e);
            }
        }
        // Read in the course name
        System.out.print("Enter the course name: ");
        String courseNum = scanner.nextLine();
        // Validate the course name and ensure that the capacity is not exceeded
        while (true) {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT * FROM course_offering WHERE course_num = ? AND yr_qtr = ?");
                preparedStatement.setString(1, courseNum);
                preparedStatement.setString(2, "25 S"); // Assuming a specific year and quarter
                ResultSet resultSet = preparedStatement.executeQuery();

                if (!resultSet.next()) {
                    System.out.println("Course does not exist, try again.");
                    courseNum = scanner.nextLine();
                } else {
                    System.out.println("Valid course found, checking capacity...");
                    // Now that we have a valid course
                    // make sure that the course is not full
                    int capacity = resultSet.getInt("cap");

                    // Check how many students are enrolled in the course
                    PreparedStatement countStatement = connection.prepareStatement(
                            "SELECT COUNT(*) AS enrolled FROM is_taking WHERE course_num = ?");
                    countStatement.setString(1, courseNum);
                    ResultSet countResultSet = countStatement.executeQuery();
                    countResultSet.next();
                    int enrolled = countResultSet.getInt("enrolled");

                    if (enrolled >= capacity) {
                        System.out.println("Course is full, please try another course.");
                        courseNum = scanner.nextLine();
                    } else {
                        // The course is valid and not full, now we have to check whether or not the
                        // prerequisites are met
                        System.out.println("Checking prerequisites for the course...");
                        // Sleep for a second to simulate checking prerequisites
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            System.out.println("An error occurred while checking prerequisites.");
                            System.out.println("SQL ERROR:");
                            System.out.println(e);
                        }
                        // Prepare a statement to get the prerequisites for the course
                        PreparedStatement preqreqStatement = connection.prepareStatement(
                                "SELECT prereq FROM courses WHERE course_num = ?");
                        preqreqStatement.setString(1, courseNum);
                        // Get the prerequisites, which should be one big string
                        ResultSet prereqResultSet = preqreqStatement.executeQuery();

                        // Check the courses that the user has already taken
                        PreparedStatement userCoursesStatement = connection.prepareStatement(
                                "SELECT course_num FROM has_taken WHERE perm = ?");
                        userCoursesStatement.setString(1, permNumber);
                        ResultSet userCoursesResultSet = userCoursesStatement.executeQuery();
                        // Now iterate through the prerequisite large string and
                        prereqResultSet.next();
                        String prereqs = prereqResultSet.getString("prereq");
                        System.out.println("Prerequisites for the course: " + prereqs);
                        if (prereqs == null || prereqs.strip().isEmpty()) {
                            System.out.println("This course has no prerequisites, you can add it directly!");
                            // The user has no prerequisites, so we can add the course
                            PreparedStatement addCourseStatement = connection.prepareStatement(
                                    "INSERT INTO is_taking (perm, course_num, yr_qtr) VALUES (?, ?, ?)");
                            addCourseStatement.setString(1, permNumber);
                            addCourseStatement.setString(2, courseNum);
                            addCourseStatement.setString(3, "25 S"); // Assuming a specific year and quarter
                            addCourseStatement.executeUpdate();

                            System.out.println("Course added successfully!");
                            break;
                        }
                        while (userCoursesResultSet.next()) {
                            String userCourse = userCoursesResultSet.getString("course_num");
                            System.out.println("Prerequisite: " + prereqs);
                            if (prereqs.contains(userCourse)) {
                                prereqs = prereqs.replace(userCourse, "");
                            }
                            if (prereqs.strip() == "") {
                                System.out.println("You have met all the prerequisites for this course!");
                                // The user has met all the prerequisites, so we can add the course
                                PreparedStatement addCourseStatement = connection.prepareStatement(
                                        "INSERT INTO is_taking (perm, course_num, yr_qtr) VALUES (?, ?, ?)");
                                addCourseStatement.setString(1, permNumber);
                                addCourseStatement.setString(2, courseNum);
                                addCourseStatement.setString(3, "25 S"); // Assuming a specific year and quarter
                                addCourseStatement.executeUpdate();

                                System.out.println("Course added successfully!");
                                break;
                            }

                        }
                        System.out.println(
                                "You have not met all the prerequisites for this course, please try another course.");
                        courseNum = scanner.nextLine();
                    }
                }
            } catch (SQLException e) {
                scanner.close();
                System.out.println("An error occurred while adding the course.");
                System.out.println("SQL ERROR:");
                System.out.println(e);
                break;
            }
        }
    }

    public static void dropCourse() {
        // Implement drop course logic here
        // For example, you can use a Scanner to read user input for course name
        System.out.print("Enter the student perm number: ");
        String permNumber = scanner.nextLine();
        // Validate the perm number
        while (true) {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT * FROM students WHERE perm = ?");
                preparedStatement.setString(1, permNumber);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (!resultSet.next()) {
                    System.out.println("Perm number does not exist, try again.");
                    permNumber = scanner.nextLine();
                } else {
                    break;
                }
            } catch (SQLException e) {
                System.out.println("SQL ERROR:");
                System.out.println(e);
            }

        }

        // Now that we have a valid perm number, we can drop a course
        System.out.print("Enter the course number you want to drop: ");
        String courseNum = scanner.nextLine();

        // Validate course number
        while (true) {
            // Check if the course exists and that the user is enrolled in it
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT * FROM is_taking WHERE course_num = ? AND perm = ?");
                preparedStatement.setString(1, courseNum);
                preparedStatement.setString(2, permNumber);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (!resultSet.next()) {
                    // No next means that there is no valid combination of course and perm number
                    System.out.println("Course does not exist or you are not enrolled in it, try again.");
                    courseNum = scanner.nextLine();
                } else {
                    // We got a valid pairing, we can drop the course
                    // Have to delete the course from the is_taking table
                    PreparedStatement dropCourseStatement = connection.prepareStatement(
                            "DELETE FROM courses WHERE course_num = ? AND perm = ?");
                    dropCourseStatement.setString(1, courseNum);
                    dropCourseStatement.setString(2, permNumber);
                    dropCourseStatement.executeUpdate();

                    // Have to reduce the number of enrolled students in the course by 1
                    PreparedStatement updateEnrolledStatement = connection.prepareStatement(
                            "UPDATE courses SET enrolled = enrolled - 1 WHERE course_num = ?");
                    updateEnrolledStatement.setString(1, courseNum);
                    updateEnrolledStatement.executeUpdate();

                    System.out.println("Course dropped successfully!");
                    break;
                }
            } catch (SQLException e) {
                scanner.close();
                System.out.println("An error occurred while dropping the course.");

                System.out.println("SQL ERROR:");
                System.out.println(e);
            }
        }
        scanner.close();
    }

}
