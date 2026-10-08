package com.college.dto;

public record SubjectRequest(String code, String name, Integer credits, Integer semester, Long departmentId) {
}
