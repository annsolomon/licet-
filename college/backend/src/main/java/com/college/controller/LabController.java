package com.college.controller;

import com.college.service.SyllabusLabService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** JAVA LAB: /api/lab/* runs the Java syllabus programs and returns their output as JSON. */
@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final SyllabusLabService service;

    public LabController(SyllabusLabService service) {
        this.service = service;
    }

    /** All fixed demos (15 syllabus topics). */
    @GetMapping("/all")
    public Map<String, List<String>> all() { return service.all(); }

    @GetMapping("/factorial")
    public Map<String, Object> factorial(@RequestParam(defaultValue = "5") int n) { return service.factorial(n); }

    @GetMapping("/fibonacci")
    public Map<String, Object> fibonacci(@RequestParam(defaultValue = "10") int count) { return service.fibonacci(count); }

    @GetMapping("/sort")
    public Map<String, Object> sort(@RequestParam String numbers) { return service.sort(numbers); }

    @GetMapping("/binary-search")
    public Map<String, Object> binarySearch(@RequestParam String numbers, @RequestParam int key) {
        return service.binarySearch(numbers, key);
    }

    @GetMapping("/shape")
    public Map<String, Object> shape(@RequestParam String type, @RequestParam int a, @RequestParam(defaultValue = "0") int b) {
        return service.shape(type, a, b);
    }

    @GetMapping("/string")
    public Map<String, Object> string(@RequestParam String text) { return service.string(text); }

    @GetMapping("/student-search")
    public Map<String, Object> studentSearch(@RequestParam(defaultValue = "") String keyword) {
        return service.studentSearch(keyword);
    }

    @GetMapping("/jdbc-compare")
    public Map<String, Object> jdbcCompare(@RequestParam(defaultValue = "1") long deptId) {
        return service.jdbcCompare(deptId);
    }
}
