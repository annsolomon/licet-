package com.college.exception;

/** Wraps low-level database failures (connection refused, SQL error) into our own type. HTTP 503/500. */
public class DatabaseException extends CollegeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
