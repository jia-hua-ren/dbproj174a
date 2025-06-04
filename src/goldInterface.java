
import java.sql.*;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;

public class goldInterface {

    static String DB_URL = "";
    final static String DB_USER = "ADMIN";
    static String DB_PASSWORD = "";
    static OracleConnection connection;
    static Scanner scanner = new Scanner(System.in);
    static String currentQuarter = "25 S"; // Assuming a specific year and quarter
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
        // First force user to login 
        String studentName = "";
        String permNumber = "";
        String[] array = new String[2];
        while (true) {
            try {
                array = login();
                break;
            } catch (Exception e) {
                System.out.println("An error occurred during login, please try again.");
                System.out.println(e);
                return; // Exit if login fails
            }
        }
        studentName = array[0];
        permNumber = array[1];

        while (true) { // Find out what action the user wants to take
            System.out.println("Welcome to the Gold Interface " + studentName + "!");
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
                    addCourse(permNumber);
                    break;
                case 2:
                    dropCourse(permNumber);
                    break;
                case 3:
                    listCurrentQuarterCourses(permNumber);
                    break;
                case 4:
                    viewPreviousQuarterGrades(permNumber);
                    break;
                case 5:
                    checkRequirements(permNumber);
                    break;
                case 6:
                    makeStudyPlan(permNumber);
                    break;
                case 7:
                    try {
                        changePin(permNumber);
                    } catch (Exception e) {
                        System.out.println("An error occurred while changing the PIN.");
                        System.out.println(e);
                    }
                    break;
                case 8:
                    System.out.println("Exiting the application.");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
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

    public static String[] login() throws Exception {
        // Implement login logic here
        // For example, you can use a Scanner to read user input for username and
        // password
        String sname = "";
        String perm = "";
        // Check that the username and password match valid entries in the database
        while (true) {
            System.out.print("Enter perm number: ");
            perm = scanner.nextLine();

            System.out.print("Enter pin: ");
            String pin = scanner.nextLine();

            // Ensure that perm and pin are both integers 
            while (!perm.matches("\\d+") || !pin.matches("\\d+")) {
                System.out.println("Invalid input. Please enter numeric values for perm number and pin.");
                System.out.print("Enter perm number: ");
                perm = scanner.nextLine();
                System.out.print("Enter pin: ");
                pin = scanner.nextLine();
            }
            // Hash the pin for security 
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(pin).getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String hashedPin = hexString.toString();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT sname FROM students WHERE perm = ? AND pin = ?");
                preparedStatement.setString(1, perm);
                preparedStatement.setString(2, hashedPin);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    System.out.println("Login successful!");
                    sname = resultSet.getString("sname");
                    break;
                    // Proceed with the application logic
                } else {
                    System.out.println("Wrong password, try again.");
                }
            } catch (SQLException e) {
                System.out.println("SQL ERROR:");
                System.out.println(e);
            }
        }

        return new String[]{sname, perm};
    }

    public static void addCourse(String permNumber) {
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
                return;
            }
        }
    }

    public static void dropCourse(String permNumber) {
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
                            "DELETE FROM is_taking WHERE course_num = ? AND perm = ?");
                    dropCourseStatement.setString(1, courseNum);
                    dropCourseStatement.setString(2, permNumber);
                    dropCourseStatement.executeUpdate();

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
    }

    public static void listCurrentQuarterCourses(String permNumber) {
        // went through validation, now list courses by looking at the is_taking table
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT course_num FROM is_taking WHERE perm = ? AND yr_qtr = ?");
            preparedStatement.setString(1, permNumber);
            preparedStatement.setString(2, currentQuarter); // Assuming currentQuarter is defined
            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("Courses enrolled in for " + currentQuarter + ":");
            while (resultSet.next()) {
                String courseNum = resultSet.getString("course_num");
                System.out.println(courseNum);
            }
        } catch (SQLException e) {
            System.out.println("SQL ERROR:");
            System.out.println(e);
        }
    }

    public static void viewPreviousQuarterGrades(String permNumber) {
        // First calculate the previous quarter based on the current quarter
        String previousQuarter = "";
        if (currentQuarter.charAt(currentQuarter.length() - 1) == 'S') {
            previousQuarter = "25 W";
        } else if (currentQuarter.charAt(currentQuarter.length() - 1) == 'W') {
            previousQuarter = "24 F";
        } else {
            previousQuarter = "24 S";
        }

        // went through validation, now list courses by looking at the has_taken table
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT course_num, grade FROM has_taken WHERE perm = ? AND yr_qtr = ?"
            );
            // Now substitute values in 
            preparedStatement.setString(1, permNumber);
            preparedStatement.setString(2, previousQuarter);

            // Execute the query
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                System.out.println("No courses found for the previous quarter: " + previousQuarter);
                return;
            }
            System.out.println("Courses and grades for " + previousQuarter + ":");
            String courseNum = resultSet.getString("course_num");
            String grade = resultSet.getString("grade");
            System.out.println("Course: " + courseNum + ", Grade: " + grade);
            while (resultSet.next()) {
                courseNum = resultSet.getString("course_num");
                grade = resultSet.getString("grade");
                System.out.println("Course: " + courseNum + ", Grade: " + grade);
            }
        } catch (Exception e) {
            System.out.println("SQL ERROR:");
            System.out.println(e);
            return;

        }

    }

    public static void checkRequirements(String permNumber) {
        // Get the set of classes that the user has already taken 
        List<String> takenCourses = new ArrayList<>();
        List<String> takingCourses = new ArrayList<>();
        try {
            PreparedStatement previousCourses = connection.prepareStatement(
                    "SELECT course_num, grade FROM has_taken WHERE perm = ?");
            previousCourses.setString(1, permNumber); // Replace with actual perm number
            ResultSet resultSet = previousCourses.executeQuery();

            List<String> passingGrades = Arrays.asList("A", "B", "C"); // Assuming these are passing grades
            while (resultSet.next()) {
                if (passingGrades.contains(resultSet.getString("grade"))) {
                    takenCourses.add(resultSet.getString("course_num"));
                }
            }

            PreparedStatement currentCourses = connection.prepareStatement(
                    "SELECT course_num FROM is_taking WHERE perm = ?");
            currentCourses.setString(1, permNumber); // Replace with actual perm number
            ResultSet resultSet2 = currentCourses.executeQuery();
            while (resultSet2.next()) {
                takingCourses.add(resultSet2.getString("course_num"));
            }
        } catch (SQLException e) {
            System.out.println("SQL ERROR:");
            System.out.println(e);
        }
        // Get the set of classes that the user has to take 
        List<String> requiredClasses = new ArrayList<>(Arrays.asList(
                "CS026", "CS130", "CS154", "CS170", "CS160"
        ));
        List<String> electiveClasses = new ArrayList<>(Arrays.asList(
                "CS010", "EC010", "EC015", "EC140", "EC152", "EC154", "CS174"
        ));
        int count = 0;
        Iterator<String> iterator = requiredClasses.iterator();
        while (iterator.hasNext()) {
            String course = iterator.next();
            if (takenCourses.contains(course)) {
                System.out.println("Required Met: " + course);
                iterator.remove();
            } else if (takingCourses.contains(course)) {
                System.out.println("Required in Progress " + currentQuarter + " : " + course);
                iterator.remove();
            } else {
                System.out.println("Required Not Met: " + course);
                count++;
            }
        }
        if (count == 0) {
            System.out.println("You have met all the required classes!");
        } else {
            System.out.println("Remaining Required Classes: " + count);
        }
        count = 0;
        Iterator<String> electiveIterator = electiveClasses.iterator();
        while (electiveIterator.hasNext()) {
            String course = electiveIterator.next();
            if (takenCourses.contains(course)) {
                System.out.println("Elective Met: " + course);
                electiveIterator.remove();
            } else if (takingCourses.contains(course)) {
                System.out.println("Elective in Progress " + currentQuarter + " : " + course);
                electiveIterator.remove();
            } else {
                System.out.println("Elective Not Met: " + course);
                count++;
            }
        }
        if (count == 0) {
            System.out.println("You have met all the elective classes!");
        } else {
            System.out.println("Remaining Elective Classes: " + (count - 2));
        }
        // Now, separate the sets into the required classes and elective classes
        // Check if the required classes are met and if the elective classes are met 
    }

    public static void makeStudyPlan(String permNumber) {
        // First make the optimal study schedule 
        List<String> fall = new ArrayList<>(Arrays.asList("EC010", "CS010", "EC015"));
        List<String> winter = new ArrayList<>(Arrays.asList("CS026", "CS130", "CS154", "EC152"));
        List<String> spring = new ArrayList<>(Arrays.asList("CS170", "CS160", "EC154"));

        // Now look at the courses that the user has already taken/is taking 
        List<String> takenCourses = new ArrayList<>();
        try {
            PreparedStatement previousCourses = connection.prepareStatement(
                    "SELECT course_num FROM has_taken WHERE perm = ?");
            previousCourses.setString(1, permNumber); // Replace with actual perm number
            ResultSet resultSet = previousCourses.executeQuery();

            while (resultSet.next()) {
                takenCourses.add(resultSet.getString("course_num"));
            }

            PreparedStatement currentCourses = connection.prepareStatement(
                    "SELECT course_num FROM is_taking WHERE perm = ?");
            currentCourses.setString(1, permNumber); // Replace with actual perm number
            ResultSet resultSet2 = currentCourses.executeQuery();
            while (resultSet2.next()) {
                takenCourses.add(resultSet2.getString("course_num"));
            }
        } catch (SQLException e) {
            System.out.println("SQL ERROR:");
            System.out.println(e);
        }

        // Now we have the list of courses that the user has taken 
        List<String> electives = new ArrayList<>(Arrays.asList(
                "EC154", "EC152", "EC015", "CS010", "EC010"
        ));
        // Find remaining electives that are needed 
        int special = 0;
        for (String course : takenCourses) {
            if (electives.contains(course)) {
                electives.remove(course);
            }
            if (fall.contains(course)) {
                fall.remove(course);
            } else if (winter.contains(course)) {
                winter.remove(course);
            } else if (spring.contains(course)) {
                spring.remove(course);
            } else {
                special++;
            }
        }

        // Special case of CS174 or EC140, remove an elective 
        for (int i = 0; i < special; i++) {
            String removed_course = electives.remove(0);
            if (removed_course.equals("EC154")) {
                spring.remove(removed_course);
            } else if (removed_course.equals("EC152")) {
                winter.remove(removed_course);
            } else if (removed_course.equals("EC015")) {
                fall.remove(removed_course);
            } else if (removed_course.equals("CS010")) {
                fall.remove(removed_course);
            } else if (removed_course.equals("EC010")) {
                winter.remove(removed_course);
            }
        }

        // Compress the study plans 
        if (fall.size() + winter.size() + spring.size() == 0) {
            // Already met the requirements 
            System.out.println("You have already met all the requirements!");
        } else if (fall.size() + winter.size() <= 5 && spring.isEmpty()) {
            // Can finish within one quarter
            List<String> allCourses = new ArrayList<>();
            allCourses.addAll(fall);
            allCourses.addAll(winter);
            System.out.println("You will need to take the following courses in the next quarter:");
            System.out.println("Courses: " + allCourses);
        } else if (fall.size() + winter.size() <= 5) {
            // Can finish within two quarters 
            List<String> allCourses = new ArrayList<>();
            allCourses.addAll(fall);
            allCourses.addAll(winter);
            System.out.println("You will need to take the following courses in the next two quarters:");
            System.out.println("Fall Quarter: " + allCourses);
            System.out.println("Winter Quarter: " + spring);
        } else {
            // Finish within three quarters
            System.out.println("You will need to take the following courses in the next three quarters:");
            System.out.println("Fall Quarter: " + fall);
            System.out.println("Winter Quarter: " + winter);
            System.out.println("Spring Quarter: " + spring);
        }
        return;

    }

    public static void changePin(String permNumber) throws Exception {
        // First, ask the user for current pin to validate 
        System.out.print("Enter current pin: ");
        String pin = scanner.nextLine();
        // Hash the current pin for security
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(String.valueOf(pin).getBytes("UTF-8"));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        String hashedPin = hexString.toString();

        // Check if the current pin is correct
        try {
            PreparedStatement checkPin = connection.prepareStatement(
                    "SELECT * FROM students WHERE perm = ? AND pin = ?");
            checkPin.setString(1, permNumber);
            checkPin.setString(2, hashedPin);
            ResultSet resultSet = checkPin.executeQuery();
            if (!resultSet.next()) {
                System.out.println("Current pin is incorrect, please try again.");
                return;
            }
        } catch (Exception e) {
            System.out.println("SQL ERROR:");
            System.out.println(e);
            return;
        }

        // Passed to this point, therefore the pin is correct 
        System.out.print("Enter new pin: ");
        String newPin = scanner.nextLine();
        System.out.print("Re-enter new pin: ");
        String confirmPin = scanner.nextLine();
        // Validate the new pin
        while (!newPin.equals(confirmPin)) {
            System.out.println("New pins do not match, please try again.");
            System.out.print("Enter new pin: ");
            newPin = scanner.nextLine();
            System.out.print("Re-enter new pin: ");
            confirmPin = scanner.nextLine();
        }

        // Hash the new pin for security
        digest = MessageDigest.getInstance("SHA-256");
        hash = digest.digest(String.valueOf(newPin).getBytes("UTF-8"));
        hexString = new StringBuilder(); 
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        hashedPin = hexString.toString();

        // Update the pin in the database
        try {
            PreparedStatement updatePin = connection.prepareStatement(
                    "UPDATE students SET pin = ? WHERE perm = ?");
            updatePin.setString(1, hashedPin);
            updatePin.setString(2, permNumber);
            int rowsAffected = updatePin.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("PIN changed successfully!");
            } else {
                System.out.println("Failed to change PIN, please try again.");
            }
        } catch (SQLException e) {
            System.out.println("SQL ERROR:");
            System.out.println(e);
            return;
        }

    }

}
