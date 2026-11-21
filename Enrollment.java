package courseenrollment;

public class Enrollment {
    private final int id;
    private final int studentId;
    private final int courseId;
    private EnrollmentStatus status;

    public Enrollment(int id, int studentId, int courseId, EnrollmentStatus status) {
        if (id < 1 || studentId < 1 || courseId < 1) {
            throw new IllegalArgumentException("Enrollment IDs must be 1 or more.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.status = status;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getCourseId() { return courseId; }
    public EnrollmentStatus getStatus() { return status; }

    public void cancel() {
        if (status == EnrollmentStatus.CANCELLED) {
            throw new IllegalStateException("That enrollment is already cancelled.");
        }
        status = EnrollmentStatus.CANCELLED;
    }

    void undoCancel() {
        status = EnrollmentStatus.ACTIVE;
    }
}
