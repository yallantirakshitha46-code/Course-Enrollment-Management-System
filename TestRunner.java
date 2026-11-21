package courseenrollment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testInvalidValues();
        testDuplicateActiveEnrollment();
        testFullCourse();
        testCancelAnotherStudentsEnrollment();
        testCancelThenEnrollAgain();
        testTwoActiveCoursesOneDistinctStudent();
        testRestartKeepsRecordsAndIds();
        testEmptyReportAndUnreadableFile();
        testPipeAndBlankRejected();
        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static CourseService newService(Path dir) throws IOException {
        return new CourseService(new FileManager(dir));
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }

    private static void testInvalidValues() throws Exception {
        CourseService s = newService(Files.createTempDirectory("t1"));
        boolean batchRejected = false;
        boolean capacityRejected = false;
        try { s.addCourse("A", "B", "Night", 5); } catch (IllegalArgumentException e) { batchRejected = true; }
        try { s.addCourse("A", "B", "Morning", 0); } catch (IllegalArgumentException e) { capacityRejected = true; }
        boolean tooBig = false;
        try { s.addCourse("A", "B", "Morning", 1001); } catch (IllegalArgumentException e) { tooBig = true; }
        check("Invalid batch, capacity 0 and capacity 1001 are rejected",
                batchRejected && capacityRejected && tooBig && s.getCourses().isEmpty());
    }

    private static void testDuplicateActiveEnrollment() throws Exception {
        CourseService s = newService(Files.createTempDirectory("t2"));
        Student st = s.registerStudent("Ananya Rao", "CSE");
        Course c = s.addCourse("Java Basics", "R Kumar", "Morning", 2);
        s.enroll(st.getId(), c.getId());
        boolean refused = false;
        try { s.enroll(st.getId(), c.getId()); } catch (IllegalStateException e) { refused = true; }
        check("Duplicate ACTIVE enrollment is refused", refused && s.getEnrollments().size() == 1);
    }

    private static void testFullCourse() throws Exception {
        CourseService s = newService(Files.createTempDirectory("t3"));
        Student a = s.registerStudent("A One", "CSE");
        Student b = s.registerStudent("B Two", "ECE");
        Course c = s.addCourse("Python Lab", "S Iyer", "Evening", 1);
        s.enroll(a.getId(), c.getId());
        boolean refused = false;
        try { s.enroll(b.getId(), c.getId()); } catch (IllegalStateException e) { refused = true; }
        check("Full course is refused and records are unchanged",
                refused && s.getEnrollments().size() == 1 && s.freeSeats(c.getId()) == 0);
    }

    private static void testCancelAnotherStudentsEnrollment() throws Exception {
        CourseService s = newService(Files.createTempDirectory("t4"));
        Student a = s.registerStudent("A One", "CSE");
        Student b = s.registerStudent("B Two", "ECE");
        Course c = s.addCourse("SQL Basics", "P Shah", "Afternoon", 3);
        Enrollment e = s.enroll(a.getId(), c.getId());
        boolean refused = false;
        try { s.cancelEnrollment(b.getId(), e.getId()); } catch (IllegalStateException ex) { refused = true; }
        check("Cancelling another student's enrollment is refused",
                refused && e.getStatus() == EnrollmentStatus.ACTIVE);
    }

    private static void testCancelThenEnrollAgain() throws Exception {
        CourseService s = newService(Files.createTempDirectory("t5"));
        Student a = s.registerStudent("A One", "CSE");
        Course c = s.addCourse("Java Basics", "R Kumar", "Morning", 1);
        Enrollment first = s.enroll(a.getId(), c.getId());
        s.cancelEnrollment(a.getId(), first.getId());
        boolean secondCancelRefused = false;
        try { s.cancelEnrollment(a.getId(), first.getId()); } catch (IllegalStateException e) { secondCancelRefused = true; }
        Enrollment second = s.enroll(a.getId(), c.getId());
        check("Cancel frees the seat, old record stays, new ACTIVE record is created",
                secondCancelRefused && second.getId() != first.getId()
                        && first.getStatus() == EnrollmentStatus.CANCELLED
                        && s.getEnrollments().size() == 2 && s.freeSeats(c.getId()) == 0);
    }

    private static void testTwoActiveCoursesOneDistinctStudent() throws Exception {
        CourseService s = newService(Files.createTempDirectory("t6"));
        Student a = s.registerStudent("Ananya Rao", "CSE");
        Course c1 = s.addCourse("Java Basics", "R Kumar", "Morning", 2);
        Course c2 = s.addCourse("SQL Basics", "P Shah", "Afternoon", 3);
        s.enroll(a.getId(), c1.getId());
        s.enroll(a.getId(), c2.getId());
        check("Two active courses report 2 enrollments and 1 distinct student",
                s.countByStatus(EnrollmentStatus.ACTIVE) == 2
                        && s.distinctActiveStudents().size() == 1);
    }

    private static void testRestartKeepsRecordsAndIds() throws Exception {
        Path dir = Files.createTempDirectory("t7");
        CourseService s = newService(dir);
        Student a = s.registerStudent("Ananya Rao", "CSE");
        Course c = s.addCourse("Java Basics", "R Kumar", "Morning", 2);
        Enrollment e = s.enroll(a.getId(), c.getId());
        s.cancelEnrollment(a.getId(), e.getId());

        CourseService again = newService(dir);                 // simulate a restart
        Student b = again.registerStudent("Rahul Mehta", "ECE");
        Enrollment e2 = again.enroll(b.getId(), c.getId());
        check("Restart loads the same records and continues IDs",
                again.getStudents().size() == 2 && again.getCourses().size() == 1
                        && again.findEnrollment(1).getStatus() == EnrollmentStatus.CANCELLED
                        && b.getId() == 2 && e2.getId() == 2);
    }

    private static void testEmptyReportAndUnreadableFile() throws Exception {
        Path dir = Files.createTempDirectory("t8");
        CourseService s = newService(dir);
        List<String> report = s.buildReport();
        boolean zeros = report.contains("Students: 0") && report.contains("Courses: 0")
                && report.contains("ACTIVE enrollments: 0")
                && report.contains("CANCELLED enrollments: 0");

        Files.writeString(dir.resolve("students.txt"), "1|Ananya Rao\n", StandardCharsets.UTF_8);
        String message = "";
        try {
            newService(dir);
        } catch (FileManager.DataFileException e) {
            message = e.getMessage();
        }
        System.out.println("      (file error shown to user: " + message + ")");
        check("Empty data gives zero counts; a bad file gives a clear file/line error",
                zeros && message.startsWith("students.txt line 1"));
    }

    private static void testPipeAndBlankRejected() throws Exception {
        CourseService s = newService(Files.createTempDirectory("t9"));
        boolean pipe = false;
        boolean blank = false;
        try { s.registerStudent("Bad|Name", "CSE"); } catch (IllegalArgumentException e) { pipe = true; }
        try { s.registerStudent("   ", "CSE"); } catch (IllegalArgumentException e) { blank = true; }
        check("Text fields reject '|' and blank values", pipe && blank && s.getStudents().isEmpty());
    }
}
