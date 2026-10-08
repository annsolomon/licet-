package com.college.controller;

import com.college.dto.SubjectRequest;
import com.college.model.Subject;
import com.college.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<Subject> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public Subject get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subject create(@RequestBody SubjectRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    public Subject update(@PathVariable long id, @RequestBody SubjectRequest r) { return service.update(id, r); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
