package courseenrollment;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final boolean ECHO = Boolean.getBoolean("echoInput");  // for scripted demos

    private static CourseService service;
    private static final Admin admin = new Admin();

    private static class InputClosedException extends RuntimeException { }

    public static void main(String[] args) {
        Path dataDir = Paths.get(args.length > 0 ? args[0] : "data");
        try {
            service = new CourseService(new FileManager(dataDir));
        } catch (IOException e) {
            System.out.println("Cannot start: " + e.getMessage());
            System.out.println("Please correct the file in the data folder and run again.");
            System.exit(1);
        }

        try {
            runMainMenu();
        } catch (InputClosedException e) {
            System.out.println("\nInput closed. Goodbye.");
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {
            throw new InputClosedException();
        }
        String line = in.nextLine().trim();
        if (ECHO) {
            System.out.println(line);
        }
        return line;
    }

    private static int readInt(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static int readChoice(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Please choose a number from " + min + " to " + max + ".");
        }
    }

    private static String readText(String label) {
        while (true) {
            String text = readLine(label + ": ");
            try {
                return User.requireText(label, text);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            String text = readLine(prompt).toLowerCase();
            if (text.equals("y")) {
                return true;
            }
            if (text.equals("n")) {
                return false;
            }
            System.out.println("Please type y or n.");
        }
    }

    private static void runMainMenu() {
        while (true) {
            System.out.println();
            System.out.println("MAIN MENU");
            System.out.println("1. Register student");
            System.out.println("2. Admin menu");
            System.out.println("3. Student menu");
            System.out.println("0. Exit");
            int choice = readChoice("Choice: ", 0, 3);
            switch (choice) {
                case 1 -> registerStudent();
                case 2 -> showAdminMenu();
                case 3 -> showStudentMenu();
                default -> {
                    System.out.println("Goodbye.");
                    return;
                }
            }
        }
    }

    private static void registerStudent() {
        String name = readText("Name");
        String department = readText("Department");
        try {
            Student student = service.registerStudent(name, department);
            System.out.println("Student registered. Your student ID is " + student.getId() + ".");
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    public static void showAdminMenu() {
        while (true) {
            System.out.println();
            System.out.println("ADMIN MENU");
            admin.showProfile();
            System.out.println("1. Add course");
            System.out.println("2. View courses");
            System.out.println("3. View students");
            System.out.println("4. View enrollments");
            System.out.println("5. View reports");
            System.out.println("0. Back");
            int choice = readChoice("Choice: ", 0, 5);
            switch (choice) {
                case 1 -> addCourse();
                case 2 -> viewCourses();
                case 3 -> viewStudents();
                case 4 -> viewAllEnrollments();
                case 5 -> viewReports();
                default -> {
                    return;
                }
            }
        }
    }

    private static void addCourse() {
        String title = readText("Course title");
        String trainer = readText("Trainer name");

        String[] batches = Course.getBatches();
        System.out.println("Batch:");
        for (int i = 0; i < batches.length; i++) {
            System.out.println((i + 1) + ". " + batches[i]);
        }
        int choice = readChoice("Choose batch: ", 1, batches.length);   // check before indexing
        String selectedBatch = batches[choice - 1];

        int capacity;
        while (true) {
            capacity = readInt("Capacity (" + Course.MIN_CAPACITY + " to "
                    + Course.MAX_CAPACITY + "): ");
            if (capacity >= Course.MIN_CAPACITY && capacity <= Course.MAX_CAPACITY) {
                break;
            }
            System.out.println("Capacity must be from " + Course.MIN_CAPACITY + " to "
                    + Course.MAX_CAPACITY + ".");
        }

        try {
            Course course = service.addCourse(title, trainer, selectedBatch, capacity);
            System.out.println("Course " + course.getId() + " saved.");
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static void viewCourses() {
        String[] batches = Course.getBatches();
        System.out.println("Show which batch?");
        System.out.println("0. All courses");
        for (int i = 0; i < batches.length; i++) {
            System.out.println((i + 1) + ". " + batches[i]);
        }
        int choice = readChoice("Choice: ", 0, batches.length);
        printCourses(choice == 0 ? null : batches[choice - 1]);
    }

    private static void printCourses(String batch) {
        boolean found = false;
        for (Course c : service.getCourses()) {
            if (batch == null || c.getBatch().equals(batch)) {
                c.showDetails(service.countActive(c.getId()));
                found = true;
            }
        }
        if (!found) {
            System.out.println("No records found.");
        }
    }

    private static void viewStudents() {
        if (service.getStudents().isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Student s : service.getStudents()) {
            s.showProfile();
        }
    }

    private static void viewAllEnrollments() {
        if (service.getEnrollments().isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Enrollment e : service.getEnrollments()) {
            printEnrollment(e, true);
        }
    }

    private static void viewReports() {
        System.out.println();
        service.showReport();
        if (readYesNo("Save report to data/report.txt? (y/n): ")) {
            try {
                Path path = service.saveReport();
                System.out.println("Report saved to " + path + ".");
            } catch (IOException e) {
                System.out.println("Could not save: " + e.getMessage());
            }
        }
    }

    public static void showStudentMenu() {
        Student student = askForStudent();
        if (student == null) {
            return;
        }
        while (true) {
            System.out.println();
            System.out.println("STUDENT MENU");
            User user = student;          
            user.showProfile();
            System.out.println("1. View courses");
            System.out.println("2. Enroll in course");
            System.out.println("3. Cancel my enrollment");
            System.out.println("4. View my enrollment history");
            System.out.println("0. Back");
            int choice = readChoice("Choice: ", 0, 4);
            switch (choice) {
                case 1 -> viewCourses();
                case 2 -> enrollInCourse(student);
                case 3 -> cancelMyEnrollment(student);
                case 4 -> viewMyHistory(student);
                default -> {
                    return;
                }
            }
        }
    }

    private static Student askForStudent() {
        while (true) {
            int id = readInt("Enter student ID (0 to go back): ");
            if (id == 0) {
                return null;
            }
            Student student = service.findStudent(id);
            if (student != null) {
                return student;
            }
            System.out.println("No student with ID " + id + ". Try again.");
        }
    }

    private static void enrollInCourse(Student student) {
        if (service.getCourses().isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        printCourses(null);
        int courseId;
        while (true) {
            courseId = readInt("Enroll in course ID (0 to go back): ");
            if (courseId == 0) {
                return;
            }
            if (service.findCourse(courseId) != null) {
                break;
            }
            System.out.println("No course with ID " + courseId + ". Try again.");
        }
        try {
            Enrollment e = service.enroll(student.getId(), courseId);
            System.out.println("Enrollment " + e.getId() + " saved. Status: " + e.getStatus());
            System.out.println("Free seats in course " + courseId + ": "
                    + service.freeSeats(courseId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static void cancelMyEnrollment(Student student) {
        boolean hasActive = false;
        for (Enrollment e : service.getEnrollmentsForStudent(student.getId())) {
            if (e.getStatus() == EnrollmentStatus.ACTIVE) {
                printEnrollment(e, false);
                hasActive = true;
            }
        }
        if (!hasActive) {
            System.out.println("You have no active enrollments.");
            return;
        }
        int enrollmentId;
        while (true) {
            enrollmentId = readInt("Cancel my enrollment ID (0 to go back): ");
            if (enrollmentId == 0) {
                return;
            }
            if (service.findEnrollment(enrollmentId) != null) {
                break;
            }
            System.out.println("No enrollment with ID " + enrollmentId + ". Try again.");
        }
        try {
            Enrollment e = service.cancelEnrollment(student.getId(), enrollmentId);
            System.out.println("Enrollment " + e.getId() + " saved. Status: " + e.getStatus());
            System.out.println("Free seats in course " + e.getCourseId() + ": "
                    + service.freeSeats(e.getCourseId()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private static void viewMyHistory(Student student) {
        List<Enrollment> mine = service.getEnrollmentsForStudent(student.getId());
        if (mine.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Enrollment e : mine) {
            printEnrollment(e, false);
        }
    }

    private static void printEnrollment(Enrollment e, boolean showStudent) {
        Course course = service.findCourse(e.getCourseId());
        String line = "Enrollment " + e.getId();
        if (showStudent) {
            Student s = service.findStudent(e.getStudentId());
            line += " | Student " + e.getStudentId() + " (" + s.getName() + ")";
        }
        line += " | Course " + e.getCourseId() + " (" + course.getTitle() + ")"
                + " | " + e.getStatus();
        System.out.println(line);
    }
}
