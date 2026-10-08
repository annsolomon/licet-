package com.college.service;

import com.college.attendance.AttendanceCalculator;
import com.college.attendance.AttendanceStatus;
import com.college.dto.AttendanceLine;
import com.college.dto.AttendanceMarkRequest;
import com.college.dto.AttendanceSummary;
import com.college.exception.ValidationException;
import com.college.repository.AttendanceRepository;
import com.college.repository.AttendanceRepository.Counts;
import com.college.repository.ClassRepository;
import com.college.repository.FacultyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ATTENDANCE flow:
 *
 *   Faculty --> Attendance page --> POST /api/attendance/mark --> AttendanceController
 *     --> AttendanceService.mark()   (validation + ONE transaction)
 *     --> AttendanceRepository       (find/create SESSION, MERGE every RECORD in a batch)
 *     --> Oracle ATTENDANCE_SESSION + ATTENDANCE_RECORD
 *     --> percentage = AttendanceCalculator.percentage(present, total)
 *     --> Dashboard / low-attendance report
 */
@Service
public class AttendanceService {

    private final AttendanceRepository repository;
    private final ClassRepository classes;
    private final FacultyRepository faculty;
    private final int lowThreshold;

    public AttendanceService(AttendanceRepository repository, ClassRepository classes, FacultyRepository faculty,
                             @Value("${app.low-attendance-threshold:75}") int lowThreshold) {
        this.repository = repository;
        this.classes = classes;
        this.faculty = faculty;
        this.lowThreshold = lowThreshold;
    }

    /**
     * @Transactional : all SQL of this method runs in ONE database transaction.
     * If any step throws, everything is rolled back (no half-saved attendance sheet).
     */
    @Transactional
    public Map<String, Object> mark(AttendanceMarkRequest r) {
        if (r == null || r.classId() == null || r.subjectId() == null || r.facultyId() == null) {
            throw new ValidationException("classId, subjectId and facultyId are required");
        }
        if (r.period() == null || r.period() < 1 || r.period() > 8) {
            throw new ValidationException("Period must be between 1 and 8");
        }
        if (r.records() == null || r.records().isEmpty()) {
            throw new ValidationException("Attendance sheet is empty");
        }
        final LocalDate date = parseDate(r.date());
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("Attendance cannot be marked for a future date");
        }
        if (classes.findAssignment(r.classId(), r.subjectId()).isEmpty()) {
            throw new ValidationException("Invalid subject: it is not taught to this class");
        }
        faculty.findById(r.facultyId()).orElseThrow(() -> new ValidationException("Invalid faculty " + r.facultyId()));

        List<Long> studentIds = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        for (AttendanceMarkRequest.Entry e : r.records()) {
            if (e == null || e.studentId() == null) {
                throw new ValidationException("Every attendance line needs a studentId");
            }
            AttendanceStatus status;
            try {
                status = AttendanceStatus.parse(e.status());
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(ex.getMessage());       // "Invalid attendance status ..."
            }
            if (!repository.studentInClass(e.studentId(), r.classId())) {
                throw new ValidationException("Student " + e.studentId() + " is not in class " + r.classId());
            }
            studentIds.add(e.studentId());
            statuses.add(status.name());
        }

        long sessionId = repository.findSession(r.classId(), r.subjectId(), date, r.period())
                .orElseGet(() -> repository.createSession(r.classId(), r.subjectId(), r.facultyId(), date, r.period()));
        repository.upsertRecords(sessionId, studentIds, statuses);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("recordsSaved", studentIds.size());
        result.put("date", date.toString());
        result.put("period", r.period());
        return result;
    }

    /** Demonstrates calling the stored procedure mark_attendance() for ONE student. */
    public Map<String, Object> markViaProcedure(long sessionId, long studentId, String status) {
        AttendanceStatus parsed;
        try {
            parsed = AttendanceStatus.parse(status);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        repository.callMarkAttendance(sessionId, studentId, parsed.name());
        return Map.of("sessionId", sessionId, "studentId", studentId, "status", parsed.name(), "via", "PL/SQL procedure mark_attendance");
    }

    // ---------------------------------------------------------------- reading
    public List<AttendanceSummary> byStudent(long studentId) {
        List<AttendanceSummary> list = toSummaries(repository.countsByStudent(studentId));
        repository.overallForStudent(studentId).ifPresent(c -> list.add(toSummary(c)));   // last row = ALL subjects
        return list;
    }

    public List<AttendanceSummary> bySubject(long subjectId) {
        return toSummaries(repository.countsBySubject(subjectId));
    }

    public List<AttendanceSummary> byClass(long classId) {
        return toSummaries(repository.countsByClass(classId));
    }

    public List<AttendanceSummary> byDepartment(long deptId) {
        return toSummaries(repository.countsByDepartment(deptId));
    }

    /** Every student, all subjects together. */
    public List<AttendanceSummary> all() {
        return toSummaries(repository.countsAll());
    }

    /** LOW ATTENDANCE REPORT: students below the threshold (default 75 %), lowest first. */
    public List<AttendanceSummary> low(Integer threshold) {
        int limit = threshold == null ? lowThreshold : threshold;
        List<AttendanceSummary> low = new ArrayList<>();
        for (Counts c : repository.countsAll()) {
            BigDecimal pct = AttendanceCalculator.percentage(c.present(), c.total());
            if (AttendanceCalculator.isLow(pct, limit)) {
                low.add(new AttendanceSummary(c.studentId(), c.studentName(), c.subjectCode(), c.total(), c.present(), pct, true));
            }
        }
        low.sort((a, b) -> a.percentage().compareTo(b.percentage()));
        return low;
    }

    public List<AttendanceLine> records(long studentId) {
        return repository.linesForStudent(studentId);
    }

    public List<AttendanceLine> sheet(long sessionId) {
        return repository.linesForSession(sessionId);
    }

    public List<Map<String, Object>> sessions(long classId, long subjectId) {
        return repository.sessions(classId, subjectId);
    }

    public int threshold() {
        return lowThreshold;
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ValidationException("Date must look like 2026-08-20");
        }
    }

    private List<AttendanceSummary> toSummaries(List<Counts> counts) {
        List<AttendanceSummary> list = new ArrayList<>();
        for (Counts c : counts) {
            list.add(toSummary(c));
        }
        return list;
    }

    private AttendanceSummary toSummary(Counts c) {
        BigDecimal pct = AttendanceCalculator.percentage(c.present(), c.total());
        return new AttendanceSummary(c.studentId(), c.studentName(), c.subjectCode(), c.total(), c.present(),
                pct, AttendanceCalculator.isLow(pct, lowThreshold));
    }
}
