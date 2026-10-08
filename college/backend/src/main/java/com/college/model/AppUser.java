package com.college.model;

/** A login account (row of APP_USER). passwordHash is never sent to the browser. */
public record AppUser(long id, String username, String passwordHash, String fullName, String role) {
}
