package com.college.dto;

/** One attendance record for display (student x date x period x subject x status). */
public record AttendanceLine(long recordId, long sessionId, long studentId, String studentName,
                             String subjectCode, String facultyName, String date, int period, String status) {
}
