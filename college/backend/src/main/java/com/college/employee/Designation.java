package com.college.employee;

/**
 * An ENUM = a fixed list of constants. The name() of each constant is exactly what is stored in
 * EMPLOYEE.DESIGNATION (the CHECK constraint chk_emp_desig lists the same four values).
 */
public enum Designation {
    PROGRAMMER("Programmer"),
    ASSISTANT_PROFESSOR("Assistant Professor"),
    ASSOCIATE_PROFESSOR("Associate Professor"),
    PROFESSOR("Professor");

    private final String label;

    Designation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Case-insensitive parse; accepts "Assistant Professor" or ASSISTANT_PROFESSOR. */
    public static Designation parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Designation is required");
        }
        String key = text.trim().toUpperCase().replace(' ', '_');
        for (Designation d : values()) {
            if (d.name().equals(key)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown designation: " + text
                + " (use PROGRAMMER, ASSISTANT_PROFESSOR, ASSOCIATE_PROFESSOR or PROFESSOR)");
    }
}
