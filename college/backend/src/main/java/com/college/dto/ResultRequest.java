package com.college.dto;

/** Ask Oracle to (re)calculate the result of one student in one subject. */
public record ResultRequest(Long studentId, Long subjectId) {
}
