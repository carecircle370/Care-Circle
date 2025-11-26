public class CareCircleUser {

    private final String username;
    private final String fullName;
    private final String role;   // "doctor", "patient", "caregiver", etc.
    private final String title;  // mainly for doctors; can be empty for others

    // Main constructor with title (used by CareCircleLoginGUI)
    public CareCircleUser(String username, String fullName, String role, String title) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.title = (title == null) ? "" : title;
    }

    // Optional 3-arg constructor for any older code
    public CareCircleUser(String username, String fullName, String role) {
        this(username, fullName, role, "");
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    // Used in DoctorHomeUI
    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        if (title != null && !title.isEmpty()) {
            return fullName + " (" + title + ", " + role + ")";
        }
        return fullName + " (" + role + ")";
    }
}
