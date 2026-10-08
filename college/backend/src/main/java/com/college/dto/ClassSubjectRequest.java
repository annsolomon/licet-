package com.college.dto;

/** Assign a subject (taught by a faculty) to a class. */
public record ClassSubjectRequest(Long classId, Long subjectId, Long facultyId) {
}
