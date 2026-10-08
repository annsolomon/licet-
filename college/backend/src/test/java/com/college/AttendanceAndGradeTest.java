package com.college;

import com.college.attendance.AttendanceCalculator;
import com.college.result.GradePointCalculator;
import com.college.result.GradePointCalculator.SubjectGrade;
import com.college.result.GradeRule;
import com.college.result.GradeScale;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceAndGradeTest {

    private static BigDecimal b(String s) { return new BigDecimal(s); }

    @Test
    void attendancePercentage() {
        assertEquals(0, b("75.00").compareTo(AttendanceCalculator.percentage(15, 20)));
        assertEquals(0, b("66.67").compareTo(AttendanceCalculator.percentage(2, 3)));
        assertNull(AttendanceCalculator.percentage(0, 0));
    }

    @Test
    void lowAttendanceIsBelowThreshold() {
        assertTrue(AttendanceCalculator.isLow(b("74.99"), 75));
        assertFalse(AttendanceCalculator.isLow(b("75.00"), 75));
        assertFalse(AttendanceCalculator.isLow(null, 75));
    }

    @Test
    void impossibleCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> AttendanceCalculator.percentage(5, 3));
    }

    @Test
    void gradeScaleLookup() {
        GradeScale scale = new GradeScale(List.of(
                new GradeRule("A", b("80"), b("89"), b("9"), true),
                new GradeRule("B", b("70"), b("79"), b("8"), true),
                new GradeRule("F", b("0"), b("39"), b("0"), false)));
        assertEquals("A", scale.lookup(b("82.6")).orElseThrow().code());   // 82.6 rounds to 83
        assertEquals("F", scale.lookup(b("10")).orElseThrow().code());
        assertTrue(scale.lookup(b("95")).isEmpty());
    }

    @Test
    void gpaIsCreditWeighted() {
        BigDecimal gpa = GradePointCalculator.gpa(List.of(
                new SubjectGrade(b("9"), 4), new SubjectGrade(b("8"), 3), new SubjectGrade(b("10"), 3)));
        assertEquals(0, b("9.00").compareTo(gpa));       // (36 + 24 + 30) / 10
        assertNull(GradePointCalculator.gpa(List.of()));
    }
}
