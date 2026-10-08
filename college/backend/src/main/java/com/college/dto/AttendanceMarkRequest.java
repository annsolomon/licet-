package com.college.dto;

import java.util.List;

/**
 * One attendance sheet: a class period plus the status of every student.
 * {"classId":1,"subjectId":2,"facultyId":2,"date":"2026-08-20","period":3,
 *  "records":[{"studentId":101,"status":"PRESENT"}, {"studentId":104,"status":"ABSENT"}]}
 */
public record AttendanceMarkRequest(Long classId, Long subjectId, Long facultyId,
                                    String date, Integer period, List<Entry> records) {

    /** One line of the sheet. */
    public record Entry(Long studentId, String status) { }
}
