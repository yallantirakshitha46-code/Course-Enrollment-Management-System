package courseenrollment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class CourseService {
    private final ArrayList<Student> students;
    private final ArrayList<Course> courses;
    private final ArrayList<Enrollment> enrollments;
    private final FileManager files;

    private int nextStudentId;
    private int nextCourseId;
    private int nextEnrollmentId;

    public CourseService(FileManager files) throws IOException {
        this.files = files;
        FileManager.LoadedData data = files.load();
        this.students = new ArrayList<>(data.students());
        this.courses = new ArrayList<>(data.courses());
        this.enrollments = new ArrayList<>(data.enrollments());

        int maxStudent = 0;
        for (Student s : students) {
            maxStudent = Math.max(maxStudent, s.getId());
        }
        int maxCourse = 0;
        for (Course c : courses) {
            maxCourse = Math.max(maxCourse, c.getId());
        }
        int maxEnrollment = 0;
        for (Enrollment e : enrollments) {
            maxEnrollment = Math.max(maxEnrollment, e.getId());
        }
        nextStudentId = maxStudent + 1;
        nextCourseId = maxCourse + 1;
        nextEnrollmentId = maxEnrollment + 1;
    }

    public List<Student> getStudents() { return Collections.unmodifiableList(students); }
    public List<Course> getCourses() { return Collections.unmodifiableList(courses); }
    public List<Enrollment> getEnrollments() { return Collections.unmodifiableList(enrollments); }

    public Student findStudent(int id) {
        for (Student s : students) {
            if (s.getId() == id) {
                return s;
            }
        }
        return null;
    }

    public Course findCourse(int id) {
        for (Course c : courses) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    public Enrollment findEnrollment(int id) {
        for (Enrollment e : enrollments) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    public List<Enrollment> getEnrollmentsForStudent(int studentId) {
        List<Enrollment> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            if (e.getStudentId() == studentId) {
                result.add(e);
            }
        }
        return result;
    }

    public int countActive(int courseId) {
        int count = 0;
        for (Enrollment e : enrollments) {
            if (e.getCourseId() == courseId && e.getStatus() == EnrollmentStatus.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    public int freeSeats(int courseId) {
        Course course = findCourse(courseId);
        if (course == null) {
            throw new IllegalArgumentException("No course with ID " + courseId + ".");
        }
        return course.getCapacity() - countActive(courseId);
    }

    public Student registerStudent(String name, String department) throws IOException {
        Student student = new Student(nextStudentId, name, department);
        students.add(student);
        try {
            files.saveStudents(students);
        } catch (IOException | RuntimeException e) {
            students.remove(student);
            throw e;
        }
        nextStudentId++;
        return student;
    }

    public Course addCourse(String title, String trainer, String batch, int capacity)
            throws IOException {
        Course course = new Course(nextCourseId, title, trainer, batch, capacity);
        courses.add(course);
        try {
            files.saveCourses(courses);
        } catch (IOException | RuntimeException e) {
            courses.remove(course);
            throw e;
        }
        nextCourseId++;
        return course;
    }

    public Enrollment enroll(int studentId, int courseId) throws IOException {
        if (findStudent(studentId) == null) {
            throw new IllegalArgumentException("No student with ID " + studentId + ".");
        }
        Course course = findCourse(courseId);
        if (course == null) {
            throw new IllegalArgumentException("No course with ID " + courseId + ".");
        }
        for (Enrollment e : enrollments) {
            if (e.getStudentId() == studentId && e.getCourseId() == courseId
                    && e.getStatus() == EnrollmentStatus.ACTIVE) {
                throw new IllegalStateException("You already have an active enrollment.");
            }
        }
        if (countActive(courseId) >= course.getCapacity()) {
            throw new IllegalStateException("This course is full. No seats are free.");
        }

        Enrollment enrollment = new Enrollment(nextEnrollmentId, studentId, courseId,
                EnrollmentStatus.ACTIVE);
        enrollments.add(enrollment);
        try {
            files.saveEnrollments(enrollments);
        } catch (IOException | RuntimeException e) {
            enrollments.remove(enrollment);
            throw e;
        }
        nextEnrollmentId++;
        return enrollment;
    }

    public Enrollment cancelEnrollment(int studentId, int enrollmentId) throws IOException {
        Enrollment enrollment = findEnrollment(enrollmentId);
        if (enrollment == null) {
            throw new IllegalArgumentException("No enrollment with ID " + enrollmentId + ".");
        }
        if (enrollment.getStudentId() != studentId) {
            throw new IllegalStateException("You can only cancel your own enrollment.");
        }
        enrollment.cancel();                       // throws if it is already cancelled
        try {
            files.saveEnrollments(enrollments);
        } catch (IOException | RuntimeException e) {
            enrollment.undoCancel();
            throw e;
        }
        return enrollment;
    }

    public HashSet<Integer> distinctActiveStudents() {
        HashSet<Integer> activeStudents = new HashSet<>();
        for (Enrollment e : enrollments) {
            if (e.getStatus() == EnrollmentStatus.ACTIVE) {
                activeStudents.add(e.getStudentId());
            }
        }
        return activeStudents;
    }

    public int countByStatus(EnrollmentStatus status) {
        int count = 0;
        for (Enrollment e : enrollments) {
            if (e.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    public List<String> buildReport() {
        List<String> lines = new ArrayList<>();
        lines.add("COURSE ENROLLMENT REPORT");
        lines.add("Students: " + students.size());
        lines.add("Courses: " + courses.size());
        lines.add("ACTIVE enrollments: " + countByStatus(EnrollmentStatus.ACTIVE));
        lines.add("CANCELLED enrollments: " + countByStatus(EnrollmentStatus.CANCELLED));
        lines.add("Distinct students with active enrollments: " + distinctActiveStudents().size());
        lines.add("Seats used per course:");
        for (Course c : courses) {
            lines.add("Course " + c.getId() + " | " + c.getTitle() + ": "
                    + countActive(c.getId()) + " of " + c.getCapacity() + " seats used");
        }
        return lines;
    }

    public void showReport() {
        for (String line : buildReport()) {
            System.out.println(line);
        }
    }

    public java.nio.file.Path saveReport() throws IOException {
        return files.saveReport(buildReport());
    }
}
