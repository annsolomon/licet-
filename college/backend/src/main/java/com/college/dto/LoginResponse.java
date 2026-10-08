package com.college.dto;

/** Returned after a successful login. The browser sends the token back in the Authorization header. */
public record LoginResponse(String token, String username, String fullName, String role) {
}
