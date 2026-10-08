package com.college.exception;

/** Thrown by Java when a duplicate is detected before touching Oracle. HTTP 409. */
public class DuplicateResourceException extends CollegeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
