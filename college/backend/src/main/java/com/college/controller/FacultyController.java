package com.college.controller;

import com.college.dto.FacultyRequest;
import com.college.model.Faculty;
import com.college.service.FacultyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyService service;

    public FacultyController(FacultyService service) {
        this.service = service;
    }

    @GetMapping
    public List<Faculty> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public Faculty get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Faculty create(@RequestBody FacultyRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    public Faculty update(@PathVariable long id, @RequestBody FacultyRequest r) { return service.update(id, r); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
