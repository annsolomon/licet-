package com.college.controller;

import com.college.dto.DepartmentRequest;
import com.college.model.Department;
import com.college.service.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Controller = receives HTTP, calls the service, returns JSON. No business logic here. */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Department> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public Department get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Department create(@RequestBody DepartmentRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    public Department update(@PathVariable long id, @RequestBody DepartmentRequest r) { return service.update(id, r); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
