package com.college.controller;

import com.college.dto.AttendanceLine;
import com.college.dto.AttendanceMarkRequest;
import com.college.dto.AttendanceSummary;
import com.college.service.AttendanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Attendance: mark a sheet, and the 5 views (student, subject, class, department, all) + low attendance. */
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, Object> mark(@RequestBody AttendanceMarkRequest r) { return service.mark(r); }

    /** Same job through the PL/SQL procedure mark_attendance (one student). */
    @PostMapping("/procedure")
    public Map<String, Object> markViaProcedure(@RequestBody Map<String, Object> body) {
        return service.markViaProcedure(((Number) body.get("sessionId")).longValue(),
                ((Number) body.get("studentId")).longValue(), String.valueOf(body.get("status")));
    }

    @GetMapping("/student/{id}")
    public List<AttendanceSummary> byStudent(@PathVariable long id) { return service.byStudent(id); }

    @GetMapping("/student/{id}/records")
    public List<AttendanceLine> records(@PathVariable long id) { return service.records(id); }

    @GetMapping("/subject/{id}")
    public List<AttendanceSummary> bySubject(@PathVariable long id) { return service.bySubject(id); }

    @GetMapping("/class/{id}")
    public List<AttendanceSummary> byClass(@PathVariable long id) { return service.byClass(id); }

    @GetMapping("/department/{id}")
    public List<AttendanceSummary> byDepartment(@PathVariable long id) { return service.byDepartment(id); }

    @GetMapping("/all")
    public List<AttendanceSummary> all() { return service.all(); }

    @GetMapping("/low")
    public List<AttendanceSummary> low(@RequestParam(required = false) Integer threshold) { return service.low(threshold); }

    @GetMapping("/sessions")
    public List<Map<String, Object>> sessions(@RequestParam long classId, @RequestParam long subjectId) {
        return service.sessions(classId, subjectId);
    }

    @GetMapping("/sessions/{id}")
    public List<AttendanceLine> sheet(@PathVariable long id) { return service.sheet(id); }

    @GetMapping("/threshold")
    public Map<String, Integer> threshold() { return Map.of("threshold", service.threshold()); }
}
