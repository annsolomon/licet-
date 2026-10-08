package com.college.dto;

/**
 * JSON sent by the React "Add Student" form:
 * {"name":"Rahul","email":"rahul@college.edu","phone":"9000000101","departmentId":1,"classId":1,"joinedYear":2025}
 * "id" is optional: when it is missing the database sequence seq_student chooses the next number.
 */
public record StudentRequest(Long id, String name, String email, String phone,
                             Long departmentId, Long classId, Integer joinedYear) {
}
