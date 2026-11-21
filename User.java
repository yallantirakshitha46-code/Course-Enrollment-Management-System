package courseenrollment;

public abstract class User {
    private final int id;      
    private String name;

    protected User(int id, String name) {
        this.id = id;
        this.name = requireText("Name", name);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText("Name", name);
    }

    public abstract void showProfile();

    static String requireText(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (value.indexOf('|') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " cannot contain '|' or line breaks.");
        }
        return value.trim();
    }
}
