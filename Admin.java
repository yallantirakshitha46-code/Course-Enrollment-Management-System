package courseenrollment;

public class Admin extends User {
    public static final int ADMIN_ID = 0;

    public Admin() {
        super(ADMIN_ID, "Admin");
    }

    @Override
    public void showProfile() {
        System.out.println("Admin | ID " + getId() + " | " + getName());
    }
}
