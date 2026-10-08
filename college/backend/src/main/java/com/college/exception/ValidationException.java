package com.college.exception;

/** General input validation failure (invalid attendance, bad month, missing field ...). HTTP 400. */
public class ValidationException extends CollegeException {
    public ValidationException(String message) {
        super(message);
    }
}
