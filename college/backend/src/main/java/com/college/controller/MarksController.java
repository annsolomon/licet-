package com.college.controller;

import com.college.config.AuthInterceptor;
import com.college.dto.BreakdownResponse;
import com.college.dto.MarkRequest;
import com.college.model.Assessment;
import com.college.model.MarkEntry;
import com.college.service.AuthService;
import com.college.service.MarksService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Raw marks (ASG/CT/CAT/SEM) and the step-by-step mark calculation. */
@RestController
@RequestMapping("/api/marks")
public class MarksController {

    private final MarksService service;

    public MarksController(MarksService service) {
        this.service = service;
    }

    @GetMapping("/assessments")
    public List<Assessment> assessments() { return service.assessments(); }

    @GetMapping
    public List<MarkEntry> marks(@RequestParam long studentId, @RequestParam(required = false) Long subjectId) {
        return service.marks(studentId, subjectId);
    }

    /** ?skipValidation=true lets the Oracle trigger (ORA-20001) reject marks above the maximum. */
    @PostMapping
    public List<MarkEntry> save(@RequestBody MarkRequest r, HttpServletRequest http,
                                @RequestParam(defaultValue = "false") boolean skipValidation) {
        AuthService.Session s = (AuthService.Session) http.getAttribute(AuthInterceptor.ATTR_USER);
        return service.save(r, s == null ? "unknown" : s.username(), skipValidation);
    }

    @GetMapping("/breakdown")
    public BreakdownResponse breakdown(@RequestParam long studentId, @RequestParam long subjectId) {
        return service.breakdown(studentId, subjectId);
    }
}
