package com.college.exception;

/** Thrown when a student id does not exist. HTTP 404. */
public class StudentNotFoundException extends CollegeException {
    public StudentNotFoundException(long studentId) {
        super("Student " + studentId + " not found");
    }
}
