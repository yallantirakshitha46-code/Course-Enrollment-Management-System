package courseenrollment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileManager {
    public static final String STUDENTS_FILE = "students.txt";
    public static final String COURSES_FILE = "courses.txt";
    public static final String ENROLLMENTS_FILE = "enrollments.txt";
    public static final String REPORT_FILE = "report.txt";

    public static class DataFileException extends IOException {
        public DataFileException(String message) {
            super(message);
        }
    }

    public record LoadedData(List<Student> students, List<Course> courses,
                             List<Enrollment> enrollments) { }

    private final Path dir;

    public FileManager(Path dir) {
        this.dir = dir;
    }

    public Path getDir() {
        return dir;
    }

    public void ensureDataFolder() throws IOException {
        Files.createDirectories(dir);
    }

    public LoadedData load() throws IOException {
        ensureDataFolder();
        List<Student> students = loadStudents();
        List<Course> courses = loadCourses();
        List<Enrollment> enrollments = loadEnrollments(students, courses);
        return new LoadedData(students, courses, enrollments);
    }

    private List<Student> loadStudents() throws IOException {
        List<String[]> rows = readRecords(STUDENTS_FILE, 3);
        List<Student> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int line = i + 1;
            String[] f = rows.get(i);
            int id = parseId(STUDENTS_FILE, line, f[0]);
            if (!ids.add(id)) {
                throw bad(STUDENTS_FILE, line, "duplicate student ID " + id);
            }
            try {
                list.add(new Student(id, f[1], f[2]));
            } catch (IllegalArgumentException e) {
                throw bad(STUDENTS_FILE, line, e.getMessage());
            }
        }
        return list;
    }

    private List<Course> loadCourses() throws IOException {
        List<String[]> rows = readRecords(COURSES_FILE, 5);
        List<Course> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int line = i + 1;
            String[] f = rows.get(i);
            int id = parseId(COURSES_FILE, line, f[0]);
            if (!ids.add(id)) {
                throw bad(COURSES_FILE, line, "duplicate course ID " + id);
            }
            int capacity;
            try {
                capacity = Integer.parseInt(f[4].trim());
            } catch (NumberFormatException e) {
                throw bad(COURSES_FILE, line, "capacity is not a number: " + f[4]);
            }
            try {
                list.add(new Course(id, f[1], f[2], f[3], capacity));
            } catch (IllegalArgumentException e) {
                throw bad(COURSES_FILE, line, e.getMessage());
            }
        }
        return list;
    }

    private List<Enrollment> loadEnrollments(List<Student> students, List<Course> courses)
            throws IOException {
        Set<Integer> studentIds = new HashSet<>();
        for (Student s : students) {
            studentIds.add(s.getId());
        }
        Map<Integer, Integer> capacities = new HashMap<>();
        for (Course c : courses) {
            capacities.put(c.getId(), c.getCapacity());
        }

        List<String[]> rows = readRecords(ENROLLMENTS_FILE, 4);
        List<Enrollment> list = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        Set<String> activePairs = new HashSet<>();
        Map<Integer, Integer> activeCounts = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            int line = i + 1;
            String[] f = rows.get(i);
            int id = parseId(ENROLLMENTS_FILE, line, f[0]);
            int studentId = parseId(ENROLLMENTS_FILE, line, f[1]);
            int courseId = parseId(ENROLLMENTS_FILE, line, f[2]);
            if (!ids.add(id)) {
                throw bad(ENROLLMENTS_FILE, line, "duplicate enrollment ID " + id);
            }
            if (!studentIds.contains(studentId)) {
                throw bad(ENROLLMENTS_FILE, line, "unknown student ID " + studentId);
            }
            if (!capacities.containsKey(courseId)) {
                throw bad(ENROLLMENTS_FILE, line, "unknown course ID " + courseId);
            }
            EnrollmentStatus status;
            try {
                status = EnrollmentStatus.valueOf(f[3].trim());
            } catch (IllegalArgumentException e) {
                throw bad(ENROLLMENTS_FILE, line, "status must be ACTIVE or CANCELLED: " + f[3]);
            }
            if (status == EnrollmentStatus.ACTIVE) {
                if (!activePairs.add(studentId + ":" + courseId)) {
                    throw bad(ENROLLMENTS_FILE, line,
                            "duplicate ACTIVE enrollment for student " + studentId
                                    + " in course " + courseId);
                }
                int count = activeCounts.merge(courseId, 1, Integer::sum);
                if (count > capacities.get(courseId)) {
                    throw bad(ENROLLMENTS_FILE, line,
                            "active enrollments exceed the capacity of course " + courseId);
                }
            }
            list.add(new Enrollment(id, studentId, courseId, status));
        }
        return list;
    }

    private List<String[]> readRecords(String fileName, int fieldCount) throws IOException {
        Path path = dir.resolve(fileName);
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(path)) {
            return rows;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String[] fields = line.split("\\|", -1);   // -1 keeps an empty last field
                if (fields.length != fieldCount) {
                    throw bad(fileName, lineNo,
                            "expected " + fieldCount + " fields but found " + fields.length);
                }
                rows.add(fields);
            }
        } catch (DataFileException e) {
            throw e;
        } catch (IOException e) {
            throw new DataFileException(fileName + ": cannot read file (" + e + ")");
        }
        return rows;
    }

    private static int parseId(String fileName, int line, String text) throws DataFileException {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 1) {
                throw bad(fileName, line, "ID must be 1 or more: " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw bad(fileName, line, "ID is not a number: " + text);
        }
    }

    private static DataFileException bad(String fileName, int line, String message) {
        return new DataFileException(fileName + " line " + line + ": " + message);
    }

    public void saveStudents(List<Student> students) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Student s : students) {
            lines.add(s.getId() + "|" + s.getName() + "|" + s.getDepartment());
        }
        writeAtomically(STUDENTS_FILE, lines);
    }

    public void saveCourses(List<Course> courses) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Course c : courses) {
            lines.add(c.getId() + "|" + c.getTitle() + "|" + c.getTrainer() + "|"
                    + c.getBatch() + "|" + c.getCapacity());
        }
        writeAtomically(COURSES_FILE, lines);
    }

    public void saveEnrollments(List<Enrollment> enrollments) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Enrollment e : enrollments) {
            lines.add(e.getId() + "|" + e.getStudentId() + "|" + e.getCourseId() + "|"
                    + e.getStatus());
        }
        writeAtomically(ENROLLMENTS_FILE, lines);
    }

    public Path saveReport(List<String> lines) throws IOException {
        writeAtomically(REPORT_FILE, lines);
        return dir.resolve(REPORT_FILE);
    }

    private void writeAtomically(String fileName, List<String> lines) throws IOException {
        ensureDataFolder();
        Path target = dir.resolve(fileName);
        Path temp = Files.createTempFile(dir, fileName, ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }
}
