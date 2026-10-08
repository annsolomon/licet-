package com.college.exception;

/** Thrown for negative marks, marks above the maximum, or missing marks. HTTP 400. */
public class InvalidMarksException extends CollegeException {
    public InvalidMarksException(String message) {
        super(message);
    }
}
