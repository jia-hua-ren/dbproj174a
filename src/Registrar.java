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
                    // case "9":
                    // testPrintMajors();
                    // break;
                    // case "10":
                    // testPrintCourseOfferings();
                    // break;
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
        // System.out.println("9. Test print majors (for debugging)");
        // System.out.println("10. Test print course offerings (for debugging)");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    // private static void testPrintMajors() {
    // String sql = "SELECT * FROM Majors";
    // try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
    // ResultSet rs = pstmt.executeQuery();
    // ResultSetMetaData rsmd = rs.getMetaData();
    // int columnCount = rsmd.getColumnCount();

    // // Print column names
    // for (int i = 1; i <= columnCount; i++) {
    // System.out.print(rsmd.getColumnName(i) + "\t");
    // }
    // System.out.println();

    // // Print each row
    // while (rs.next()) {
    // for (int i = 1; i <= columnCount; i++) {
    // System.out.print(rs.getString(i) + "\t");
    // }
    // System.out.println();
    // }
    // } catch (SQLException e) {
    // System.err.println("Error: " + e.getMessage());
    // }
    // }

    // private static void testPrintCourseOfferings() {
    // String sql = "SELECT * FROM course_offering";
    // try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
    // ResultSet rs = pstmt.executeQuery();
    // ResultSetMetaData rsmd = rs.getMetaData();
    // int columnCount = rsmd.getColumnCount();

    // // Print column names
    // for (int i = 1; i <= columnCount; i++) {
    // System.out.print(rsmd.getColumnName(i) + "\t");
    // }
    // System.out.println();

    // // Print each row
    // while (rs.next()) {
    // for (int i = 1; i <= columnCount; i++) {
    // System.out.print(rs.getString(i) + "\t");
    // }
    // System.out.println();
    // }
    // } catch (SQLException e) {
    // System.err.println("Error: " + e.getMessage());
    // }
    // }

    private static void addStudentToCourse() {
        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine().trim();
        System.out.print("Enter course ID (current quarter only, 25 S): ");
        String courseId = scanner.nextLine().trim();

        String courseYrQtr = "25 S"; // Hardcoded for current quarter as per requirements

        String error = "";

        try {
            // Step 1: Check if student exists
            String checkSql = "SELECT 1 FROM Students WHERE perm = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkSql);
            error = "Error checking student: ";
            checkStmt.setInt(1, Integer.parseInt(studentId));
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Student with ID " + studentId + " does not exist.");
                return; // Exit early
            }

            // Step 2: check if student is already enrolled in 5 courses
            error = "Error counting enrolled courses: ";
            String countSql = "SELECT COUNT(*) AS enrolled FROM is_taking WHERE perm = ?";
            int count = 0;
            PreparedStatement countStmt = connection.prepareStatement(countSql);
            countStmt.setInt(1, Integer.parseInt(studentId));
            rs = countStmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("enrolled");
            }

            if (count == 5) {
                System.out.println("Cannot enroll: student is already enrolled in 5 courses.");
                return; // Exit early
            }

            // Step 3: Check if course exists and is in current quarter
            error = "Error checking course: ";
            String courseCheckSql = "SELECT 1 FROM Course_offering WHERE course_num = ? AND yr_qtr = ?";

            PreparedStatement courseCheckStmt = connection.prepareStatement(courseCheckSql);
            courseCheckStmt.setString(1, courseId);
            courseCheckStmt.setString(2, courseYrQtr);
            rs = courseCheckStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Course with ID " + courseId + " for " + courseYrQtr + " does not exist.");
                return; // Exit early
            }

            // Step 4: Check if student already enrolled in the course
            error = "Error checking existing enrollment: ";
            String existingEnrollmentSql = "SELECT 1 FROM is_taking WHERE perm = ? AND course_num = ? AND yr_qtr = ?";
            PreparedStatement existingEnrollmentStmt = connection.prepareStatement(existingEnrollmentSql);
            existingEnrollmentStmt.setInt(1, Integer.parseInt(studentId));
            existingEnrollmentStmt.setString(2, courseId);
            existingEnrollmentStmt.setString(3, courseYrQtr);
            rs = existingEnrollmentStmt.executeQuery();
            if (rs.next()) {
                System.out.println("Cannot enroll: student is already enrolled in course " + courseId + " "
                        + courseYrQtr + ".");
                return; // Exit early
            }

            // Step 5: Check if course is already taken with passing grade
            error = "Error checking previous enrollment: ";
            String previousEnrollmentSql = "SELECT grade FROM has_taken WHERE perm = ? AND course_num = ?";
            PreparedStatement previousEnrollmentStmt = connection.prepareStatement(previousEnrollmentSql);
            previousEnrollmentStmt.setString(1, studentId);
            previousEnrollmentStmt.setString(2, courseId);
            rs = previousEnrollmentStmt.executeQuery();
            while (rs.next()) {
                String grade = rs.getString("grade");
                if (!(grade.contains("F") || grade.contains("D") || grade.equals("C-"))) {
                    System.out.println("Cannot enroll: student has already taken course " + courseId
                            + " with passing grade " + grade + ".");
                    return; // Exit early
                }
            }

            // Step 6: check if student has taken prerequisite courses with passing grade
            error = "Error checking prerequisites: ";
            String prereqSql = "SELECT prereq FROM courses WHERE course_num = ?";
            PreparedStatement prereqStmt = connection.prepareStatement(prereqSql);
            prereqStmt.setString(1, courseId);
            rs = prereqStmt.executeQuery();
            rs.next();
            String prereq = rs.getString("prereq");

            String takenCourseSql = "SELECT grade, course_num FROM has_taken WHERE perm = ?";
            PreparedStatement takenCourseStmt = connection.prepareStatement(takenCourseSql);
            takenCourseStmt.setString(1, studentId);
            ResultSet takenCourseRS = takenCourseStmt.executeQuery();

            while (takenCourseRS.next() && prereq != null) {
                String takenCourse = takenCourseRS.getString("course_num");
                String takenGrade = takenCourseRS.getString("grade");
                if (prereq.contains(takenCourse)) {
                    // Check if the taken course has a passing grade
                    if (!(takenGrade.contains("F") || takenGrade.contains("D") || takenGrade.equals("C-"))) {
                        // Remove the prerequisite from the array
                        prereq = prereq.replace(takenCourse, "").trim();
                    }
                }
                if (prereq.isEmpty()) {
                    break; // No more prerequisites to check
                }
            }

            if (prereq != null && !prereq.isEmpty()) {
                System.out.println(
                        "Cannot enroll: student has not completed prerequisite courses with passing grade: " + prereq);
                return; // Exit early
            }

            // Step 7: Check course capacity
            error = "Error retrieving course cap: ";
            String capSql = "SELECT cap FROM Course_offering WHERE course_num = ?";
            int courseCap = 0;
            PreparedStatement capStmt = connection
                    .prepareStatement(capSql);
            capStmt.setString(1, courseId);
            rs = capStmt.executeQuery();
            if (rs.next()) {
                courseCap = rs.getInt("cap");
            } else {
                System.out.println("Course not found while checking cap.");
                return;
            }

            // Step 8: Count current enrolled students
            error = "Error counting enrolled students: ";
            String enrolledCountSql = "SELECT COUNT(*) AS enrolled FROM is_taking WHERE course_num = ? AND yr_qtr = ?";
            int enrolled = 0;
            PreparedStatement enrolledCountStmt = connection.prepareStatement(
                    enrolledCountSql);
            enrolledCountStmt.setString(1, courseId);
            enrolledCountStmt.setString(2, courseYrQtr);
            rs = enrolledCountStmt.executeQuery();
            if (rs.next()) {
                enrolled = rs.getInt("enrolled");
            }

            // Step 9: Compare count with cap
            if (enrolled >= courseCap) {
                System.out.println("Cannot enroll: course is full (cap = " + courseCap + ").");
                return;
            }
            // Step 10: Insert into is_taking table
            error = "Error inserting into is_taking: ";
            String sql = "INSERT INTO is_taking (perm, course_num, yr_qtr) VALUES (?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(
                    sql);
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            pstmt.setString(3, courseYrQtr); // Assuming current year and quarter
            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println(
                        "Student " + studentId + " added to course " + courseId + " " + courseYrQtr + " successfully.");
            }
        } catch (SQLException e) {
            System.err.println(error + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid student ID format: " + e.getMessage());
        }
    }

    private static void dropStudentFromCourse() {
        String error = "";

        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine().trim();

        System.out.print("Enter course ID: ");
        String courseId = scanner.nextLine().trim();

        String courseYrQtr = "25 S"; // Hardcoded for current quarter as per requirements

        try {
            // Step 1: Check if student exists
            error = "Error checking student: ";
            String checkSql = "SELECT 1 FROM Students WHERE perm = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setInt(1, Integer.parseInt(studentId));
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Student with ID " + studentId + " does not exist.");
                return; // Exit early
            }

            // Step 2: Check if course exists
            error = "Error checking course: ";
            String courseCheckSql = "SELECT 1 FROM Course_offering WHERE course_num = ? AND yr_qtr = ?";
            PreparedStatement courseCheckStmt = connection.prepareStatement(courseCheckSql);
            courseCheckStmt.setString(1, courseId);
            courseCheckStmt.setString(2, courseYrQtr);
            rs = courseCheckStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Course with ID " + courseId + " for " + courseYrQtr + " does not exist.");
                return; // Exit early
            }

            // Step 3: Check if student is enrolled in the course
            error = "Error checking enrollment: ";
            String enrollmentCheckSql = "SELECT 1 FROM is_taking WHERE perm = ? AND course_num = ? AND yr_qtr = ?";
            PreparedStatement enrollmentCheckStmt = connection.prepareStatement(enrollmentCheckSql);
            enrollmentCheckStmt.setInt(1, Integer.parseInt(studentId));
            enrollmentCheckStmt.setString(2, courseId);
            enrollmentCheckStmt.setString(3, courseYrQtr);
            rs = enrollmentCheckStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Student " + studentId + " is not enrolled in course " + courseId + " "
                        + courseYrQtr + ".");
                return; // Exit early
            }

            // Step 4: Check if this is the only course the student is taking
            error = "Error counting enrolled courses: ";
            String countSql = "SELECT COUNT(*) AS enrolled FROM is_taking WHERE perm = ?";
            int count = 0;
            PreparedStatement countStmt = connection.prepareStatement(countSql);
            countStmt.setInt(1, Integer.parseInt(studentId));
            rs = countStmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("enrolled");
            }
            if (count == 1) {
                System.out.println("Cannot drop: this is the only course the student is taking.");
                return; // Exit early
            }

            // Step 4: Delete from is_taking table
            error = "Error deleting from is_taking: ";
            String sql = "DELETE FROM is_taking WHERE perm=? AND course_num=? AND yr_qtr=?";

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, Integer.parseInt(studentId));
            pstmt.setString(2, courseId);
            pstmt.setString(3, courseYrQtr); // Assuming current year and quarter
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println(
                        "Student " + studentId + " dropped course " + courseId + " " + courseYrQtr + " successfully.");
            }

        } catch (SQLException e) {
            System.err.println(error + e.getMessage());
            return;
        }

    }

    private static void listCoursesForStudent() {
        System.out.print("Enter student ID: ");
        String error = "";
        String studentId = scanner.nextLine().trim();

        try {
            // Step 1: Check if student exists
            error = "Error checking student: ";
            String checkSql = "SELECT 1 FROM Students WHERE perm = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setInt(1, Integer.parseInt(studentId));
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Student with ID " + studentId + " does not exist.");
                return; // Exit early
            }

            // Step 2: Print course taken by student
            error = "Error retrieving taken courses: ";
            String hasTakenSql = "SELECT course_num, yr_qtr FROM has_taken WHERE perm = ?";
            PreparedStatement hasTakenStmt = connection.prepareStatement(hasTakenSql);
            hasTakenStmt.setInt(1, Integer.parseInt(studentId));
            rs = hasTakenStmt.executeQuery();

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            if (!rs.isBeforeFirst()) {
                System.out.println("No courses found for student " + studentId + ".");
                return; // Exit early
            }

            System.out.println("Courses for student " + studentId + ":");

            // Print column names
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rsmd.getColumnName(i) + "\t");
            }
            System.out.println();

            // Print each row
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }

        } catch (SQLException e) {
            System.err.println(error + e.getMessage());
            return;
        }
    }

    private static void listGradesForStudent() {
        System.out.print("Enter student ID: ");
        String error = "";
        String studentId = scanner.nextLine().trim();

        System.out.print("Enter quarter (e.g., '25 S' for Spring 2025, or 'all' for all quarters): ");
        String quarterInput = scanner.nextLine().trim();

        try {
            // Step 1: Check if student exists
            error = "Error checking student: ";
            String checkSql = "SELECT 1 FROM Students WHERE perm = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setInt(1, Integer.parseInt(studentId));
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Student with ID " + studentId + " does not exist.");
                return; // Exit early
            }

            // Step 2: Print course and grades taken by student
            error = "Error retrieving taken courses: ";
            String hasTakenSql;
            PreparedStatement hasTakenStmt;
            if (quarterInput.equalsIgnoreCase("all")) {
                hasTakenSql = "SELECT grade, course_num, yr_qtr FROM has_taken WHERE perm = ?";
                hasTakenStmt = connection.prepareStatement(hasTakenSql);
                hasTakenStmt.setInt(1, Integer.parseInt(studentId));
            } else if (quarterInput.matches("\\d\\d [FSW]")) {
                hasTakenSql = "SELECT grade, course_num FROM has_taken WHERE perm = ? AND yr_qtr = ?";
                hasTakenStmt = connection.prepareStatement(hasTakenSql);
                hasTakenStmt.setInt(1, Integer.parseInt(studentId));
                hasTakenStmt.setString(2, quarterInput);
            } else {
                System.out.println(
                        "Invalid quarter format. Please use '25 S' for Spring 2025 or 'all' for all quarters.");
                return; // Exit early
            }

            rs = hasTakenStmt.executeQuery();

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            if (!rs.isBeforeFirst()) {
                System.out.println("No courses found for student " + studentId + ".");
                return; // Exit early
            }

            System.out.println("Courses for student " + studentId + " for " + quarterInput + " quarter:");

            // Print column names
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rsmd.getColumnName(i) + "\t");
            }
            System.out.println();

            // Print each row
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }

        } catch (SQLException e) {
            System.err.println(error + e.getMessage());
            return;
        }
    }

    private static void generateClassList() {
        // list all students in a course
        System.out.print("Enter course ID: ");
        String courseId = scanner.nextLine().trim();

        System.out.print("Enter quarter (e.g., '25 S' for Spring 2025): ");
        String quarterInput = scanner.nextLine().trim();

        if (!quarterInput.matches("\\d\\d [FSW]")) {
            System.out.println("Invalid quarter format. Please use '25 S' for Spring 2025.");
            return; // Exit early
        }

        // Step 1: Check if course exists
        String error = "";
        try {
            error = "Error checking course: ";
            String courseCheckSql = "SELECT 1 FROM Course_offering WHERE course_num = ? AND yr_qtr = ?";
            PreparedStatement courseCheckStmt = connection.prepareStatement(courseCheckSql);
            courseCheckStmt.setString(1, courseId);
            courseCheckStmt.setString(2, quarterInput);
            ResultSet rs = courseCheckStmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Course with ID " + courseId + " for " + quarterInput + " does not exist.");
                return; // Exit early
            }

            // Step 2: Retrieve class list
            error = "Error retrieving class list: ";

            String classListSql;
            PreparedStatement classListStmt;
            // current quarter is hardcoded as "25 S" in the requirements
            if (quarterInput.equals("25 S")) {
                classListSql = "SELECT S.perm, S.sname FROM is_taking I, Students S WHERE I.perm = S.perm AND course_num = ? AND yr_qtr = ?";
            } else {
                classListSql = "SELECT S.perm, S.sname, I.grade FROM has_taken I, Students S WHERE I.perm = S.perm AND course_num = ? AND yr_qtr = ?";
            }

            classListStmt = connection.prepareStatement(classListSql);
            classListStmt.setString(1, courseId);
            classListStmt.setString(2, quarterInput);
            rs = classListStmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            if (!rs.isBeforeFirst()) {
                System.out.println("No students found for course " + courseId + " in " + quarterInput + ".");
                return; // Exit early
            }
            System.out.println("Class list for course " + courseId + " in " + quarterInput + ":");
            // Print column names
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rsmd.getColumnName(i) + "\t");
            }
            System.out.println();
            // Print each row
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }

        } catch (SQLException e) {
            System.err.println(error + e.getMessage());
            return;
        }
    }

    private static void enterGradesFromFile() {
        System.out.print("Enter filename: ");
        String filename = scanner.nextLine().trim();

        try (BufferedReader fileReader = new BufferedReader(new FileReader(filename))) {
            String line;

            line = fileReader.readLine();
            if (line == null || !line.startsWith("Course code:")) {
                System.err.println("Invalid file format: missing course code");
                return;
            }
            String courseCode = line.split(":")[1].trim();

            // Read course quarter
            line = fileReader.readLine();
            if (line == null || !line.startsWith("Course quarter:")) {
                System.err.println("Invalid file format: missing course quarter");
                return;
            }
            String courseQuarter = line.split(":")[1].trim();

            System.out.println("Processing grades for course " + courseCode + " in quarter " + courseQuarter);

            // Skip header line
            fileReader.readLine(); // perm----grade

            String insertSql = "INSERT INTO has_taken (perm, course_num, yr_qtr, grade) " +
                    "SELECT ?, ?, ?, ? FROM DUAL " +
                    "WHERE NOT EXISTS (" +
                    "  SELECT 1 FROM has_taken WHERE perm = ? AND course_num = ? AND yr_qtr = ?)";

            PreparedStatement stmt = connection.prepareStatement(insertSql);

            // UPDATE if already exists
            String updateSql = "UPDATE has_taken SET grade = ? WHERE perm = ? AND course_num = ? AND yr_qtr = ?";
            PreparedStatement updateStmt = connection.prepareStatement(updateSql);

            while ((line = fileReader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                String[] parts = line.split("----");
                if (parts.length != 2) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }

                String perm = parts[0].trim();
                String grade = parts[1].trim();

                stmt.setInt(1, Integer.parseInt(perm));
                stmt.setString(2, courseCode);
                stmt.setString(3, courseQuarter);
                stmt.setString(4, grade);
                stmt.setInt(5, Integer.parseInt(perm));
                stmt.setString(6, courseCode);
                stmt.setString(7, courseQuarter);

                stmt.executeUpdate();

                updateStmt.setString(1, grade);
                updateStmt.setInt(2, Integer.parseInt(perm));
                updateStmt.setString(3, courseCode);
                updateStmt.setString(4, courseQuarter);

                updateStmt.executeUpdate();
                System.out.println("Processed grade for " + perm + ": " + grade);
            }

        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void requestTranscript() {
        // similar to list courses and list grades?
        // list all courses and grades for a student
        // major, name
        // dont worry about gpa

        System.out.print("Enter student ID: ");
        String studentId = scanner.nextLine().trim();

        // Step 1: Check if student exists
        String error = "";
        try {
            error = "Error checking student: ";
            String checkSql = "SELECT sname, mname FROM Students WHERE perm = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setInt(1, Integer.parseInt(studentId));
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("Student with ID " + studentId + " does not exist.");
                return; // Exit early
            }

            rs.next();
            String studentName = rs.getString("sname");
            String majorName = rs.getString("mname");
            System.out.println(
                    "Transcript for student " + studentId + " (" + studentName + ", Major: " + majorName + ")");

            System.out.println();

            // Step 2: Retrieve student details
            error = "Error retrieving student current quarter details: ";
            String currentQuarterSql = "SELECT course_num FROM is_taking WHERE perm = ?";

            PreparedStatement currentQuarterStmt = connection.prepareStatement(currentQuarterSql);
            currentQuarterStmt.setInt(1, Integer.parseInt(studentId));
            rs = currentQuarterStmt.executeQuery();
            System.out.println("Current courses for 25 S: ");
            if (!rs.isBeforeFirst()) {
                System.out.println("-");
            } else {
                while (rs.next()) {
                    System.out.println("Course: " + rs.getString("course_num"));
                }
            }

            // get all past quarters
            String pastQuartersSql = "SELECT DISTINCT yr_qtr FROM course_offering WHERE yr_qtr <> '25 S'";
            PreparedStatement pastQuartersStmt = connection.prepareStatement(pastQuartersSql);
            ResultSet pastQuartersRS = pastQuartersStmt.executeQuery();

            while (pastQuartersRS.next()) {
                String quarter = pastQuartersRS.getString("yr_qtr");
                System.out.println("Past quarter: " + quarter);

                // Step 3: Retrieve courses and grades for each past quarter
                String pastCoursesSql = "SELECT course_num, grade FROM has_taken WHERE perm = ? AND yr_qtr = ?";
                PreparedStatement pastCoursesStmt = connection.prepareStatement(pastCoursesSql);
                pastCoursesStmt.setInt(1, Integer.parseInt(studentId));
                pastCoursesStmt.setString(2, quarter);
                ResultSet pastCoursesRS = pastCoursesStmt.executeQuery();

                if (!pastCoursesRS.isBeforeFirst()) {
                    System.out.println("-");
                } else {
                    while (pastCoursesRS.next()) {
                        String courseNum = pastCoursesRS.getString("course_num");
                        String grade = pastCoursesRS.getString("grade");
                        System.out.println("Course: " + courseNum + ", Grade: " + grade);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println(error + e.getMessage());
            return;
        }
    }

    private static void generateGradeMailer() {
        // For all student, for the quarter chosen generate a mailer like “(name), your
        // grade for CSxxx is A, your grade for ECExxx is A etc.”

        System.out.print("Enter quarter (e.g., '25 S' for Spring 2025): ");
        String quarterInput = scanner.nextLine().trim();

        String error = "";

        if (!quarterInput.matches("\\d\\d [FSW]")) {
            System.out.println("Invalid quarter format. Please use '25 S' for Spring 2025.");
            return; // Exit early
        }

        try {
            error = "Error retrieving all students: ";
            String studentSql = "SELECT perm, sname FROM Students";
            PreparedStatement studentStmt = connection.prepareStatement(studentSql);

            ResultSet rs = studentStmt.executeQuery();
            if (!rs.isBeforeFirst()) {
                System.out.println("No students found.");
                return; // Exit early
            }
            while (rs.next()) {
                int studentId = rs.getInt("perm");
                String studentName = rs.getString("sname");

                // Retrieve grades for the specified quarter
                error = "Error retrieving courses taken and grades: ";
                String gradesSql = "SELECT course_num, grade FROM has_taken WHERE perm = ? AND yr_qtr = ?";
                PreparedStatement gradesStmt = connection.prepareStatement(gradesSql);
                gradesStmt.setInt(1, studentId);
                gradesStmt.setString(2, quarterInput);
                ResultSet gradesRS = gradesStmt.executeQuery();

                StringBuilder mailerContent = new StringBuilder(
                        studentName + ", your grades for " + quarterInput + " are:\n");
                boolean hasGrades = false;

                while (gradesRS.next()) {
                    hasGrades = true;
                    String courseNum = gradesRS.getString("course_num");
                    String grade = gradesRS.getString("grade");
                    mailerContent.append("Course: ").append(courseNum).append(", Grade: ").append(grade).append("\n");
                }

                if (hasGrades) {
                    System.out.println(mailerContent.toString());
                } else {
                    System.out.println(studentName + ", you have no grades for " + quarterInput + ".\n");
                }
            }
        } catch (SQLException e) {
            System.err.println(error + e.getMessage());
            return;
        }

    }
}
