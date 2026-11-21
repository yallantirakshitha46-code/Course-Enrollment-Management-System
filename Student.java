package courseenrollment;

public class Student extends User {
    private String department;

    public Student(int id, String name, String department) {
        super(id, name);
        if (id < 1) {
            throw new IllegalArgumentException("Student ID must be 1 or more.");
        }
        this.department = requireText("Department", department);
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = requireText("Department", department);
    }

    @Override
    public void showProfile() {
        System.out.println("Student | ID " + getId() + " | " + getName()
                + " | Department: " + department);
    }
}
