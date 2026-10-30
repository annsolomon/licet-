package com.college.dto;

/**
 * ONE absent student in ONE period: one row per absentee (never a grouped, comma-joined list).
 * sno = running number inside the period (1, 2, 3 ...), so the screen reads like a roll call.
 */
public record AbsenteeLine(int sno, String date, int period, long sessionId,
                           long classId, String className, String subjectCode, String subjectName,
                           String facultyName, long studentId, String studentName, String status) {
}
