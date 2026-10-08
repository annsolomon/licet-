package com.college.controller;

import com.college.dto.StudentRequest;
import com.college.model.Student;
import com.college.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET /api/students?search=ra&mode=java|sql   (java = GenericSearch in memory, sql = WHERE ... LIKE)
 * POST /api/students?skipChecks=true          (demo: let ORACLE reject duplicates instead of Java)
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Student> list(@RequestParam(required = false) String search,
                              @RequestParam(defaultValue = "java") String mode,
                              @RequestParam(required = false) Long departmentId,
                              @RequestParam(required = false) Long classId) {
        if (departmentId != null) {
            return service.findByDepartment(departmentId);
        }
        if (classId != null) {
            return service.findByClass(classId);
        }
        if (search != null && !search.isBlank()) {
            return "sql".equalsIgnoreCase(mode) ? service.searchSql(search) : service.search(search);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Student get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Student create(@RequestBody StudentRequest r, @RequestParam(defaultValue = "false") boolean skipChecks) {
        return service.create(r, skipChecks);
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable long id, @RequestBody StudentRequest r) { return service.update(id, r); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
