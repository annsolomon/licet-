package com.college.controller;

import com.college.dto.ClassRequest;
import com.college.dto.ClassSubjectRequest;
import com.college.model.ClassSection;
import com.college.model.ClassSubject;
import com.college.service.ClassService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Classes (sections) and the subject + faculty assigned to each class. */
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService service;

    public ClassController(ClassService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClassSection> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public ClassSection get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassSection create(@RequestBody ClassRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    public ClassSection update(@PathVariable long id, @RequestBody ClassRequest r) { return service.update(id, r); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }

    @GetMapping("/assignments")
    public List<ClassSubject> assignments(@RequestParam(required = false) Long classId) { return service.assignments(classId); }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassSubject assign(@RequestBody ClassSubjectRequest r) { return service.assign(r); }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable long id) { service.unassign(id); }
}
