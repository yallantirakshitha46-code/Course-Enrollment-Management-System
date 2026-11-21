package courseenrollment;

public class Course {
    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 1000;

    private static final String[] BATCHES = {"Morning", "Afternoon", "Evening"};

    private final int id;
    private final int capacity;
    private final String title;
    private final String trainer;
    private final String batch;

    public Course(int id, String title, String trainer, String batch, int capacity) {
        if (id < 1) {
            throw new IllegalArgumentException("Course ID must be 1 or more.");
        }
        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                    "Capacity must be from " + MIN_CAPACITY + " to " + MAX_CAPACITY + ".");
        }
        if (!isValidBatch(batch)) {
            throw new IllegalArgumentException("Batch must be Morning, Afternoon, or Evening.");
        }
        this.id = id;
        this.capacity = capacity;
        this.title = User.requireText("Title", title);
        this.trainer = User.requireText("Trainer name", trainer);
        this.batch = batch;
    }

    public static String[] getBatches() {
        return BATCHES.clone();
    }

    public static boolean isValidBatch(String batch) {
        for (String b : BATCHES) {
            if (b.equals(batch)) {
                return true;
            }
        }
        return false;
    }

    public int getId() { return id; }
    public int getCapacity() { return capacity; }
    public String getTitle() { return title; }
    public String getTrainer() { return trainer; }
    public String getBatch() { return batch; }

    public void showDetails(int activeCount) {
        System.out.println(id + " | " + title + " | Trainer: " + trainer + " | " + batch
                + " | Capacity: " + capacity + " | Active: " + activeCount
                + " | Free seats: " + (capacity - activeCount));
    }
}
