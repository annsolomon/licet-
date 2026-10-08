package com.college.model;

/** One row of CLASS_SUBJECT: faculty X teaches subject Y to class Z. */
public record ClassSubject(long id, long classId, String className,
                           long subjectId, String subjectCode, String subjectName,
                           long facultyId, String facultyName) {
}
