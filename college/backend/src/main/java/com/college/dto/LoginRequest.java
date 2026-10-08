package com.college.dto;

/**
 * DTO = Data Transfer Object: the exact shape of the JSON that travels between React and Spring.
 * {"username":"admin","password":"admin123"}  ->  @RequestBody LoginRequest
 */
public record LoginRequest(String username, String password) {
}
