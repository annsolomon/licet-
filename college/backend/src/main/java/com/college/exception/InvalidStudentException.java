package com.college.exception;

/** Thrown when student data fails Java validation (bad name, e-mail, missing department ...). HTTP 400. */
public class InvalidStudentException extends CollegeException {
    public InvalidStudentException(String message) {
        super(message);
    }
}
