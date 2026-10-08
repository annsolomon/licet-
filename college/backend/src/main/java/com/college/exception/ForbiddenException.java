package com.college.exception;

/** Logged in, but the role is not allowed to do this. HTTP 403. */
public class ForbiddenException extends CollegeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
