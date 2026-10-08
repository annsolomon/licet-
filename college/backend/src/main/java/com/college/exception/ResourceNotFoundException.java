package com.college.exception;

/** Generic "row not found" (department, subject, employee ...). HTTP 404. */
public class ResourceNotFoundException extends CollegeException {
    public ResourceNotFoundException(String what, Object id) {
        super(what + " " + id + " not found");
    }
}
