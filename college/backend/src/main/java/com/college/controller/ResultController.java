package com.college.controller;

import com.college.dto.GradeRequest;
import com.college.dto.ResultRequest;
import com.college.dto.StudentResultSummary;
import com.college.model.ResultRow;
import com.college.result.GradeRule;
import com.college.service.ResultService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Results (calculated by Oracle), SGPA / CGPA, top performers and the configurable grade scale. */
@RestController
@RequestMapping("/api")
public class ResultController {

    private final ResultService service;

    public ResultController(ResultService service) {
        this.service = service;
    }

    @GetMapping("/results")
    public List<ResultRow> list(@RequestParam(required = false) Integer semester,
                                @RequestParam(required = false) Long departmentId) {
        return service.list(semester, departmentId);
    }

    @GetMapping("/results/student/{id}")
    public StudentResultSummary summary(@PathVariable long id) { return service.summary(id); }

    @PostMapping("/results/calculate")
    public List<ResultRow> calculate(@RequestBody ResultRequest r) { return service.calculate(r); }

    @PostMapping("/results/calculate-all")
    public Map<String, Object> calculateAll() {
        return Map.of("calculated", service.calculateAll(), "via", "PL/SQL procedure calculate_all_results");
    }

    @GetMapping("/results/top")
    public List<Map<String, Object>> top(@RequestParam(defaultValue = "5") int limit) { return service.topPerformers(limit); }

    @GetMapping("/grades")
    public List<GradeRule> grades() { return service.grades(); }

    @PutMapping("/grades/{code}")
    public List<GradeRule> updateGrade(@PathVariable String code, @RequestBody GradeRequest r) {
        return service.updateGrade(code, r);
    }
}
