package com.college.exception;

/** Missing / wrong login token or wrong password. HTTP 401. */
public class UnauthorizedException extends CollegeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
