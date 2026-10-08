package com.college.controller;

import com.college.dto.EmployeeRequest;
import com.college.model.EmployeeRecord;
import com.college.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public List<EmployeeRecord> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public EmployeeRecord get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeRecord create(@RequestBody EmployeeRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    public EmployeeRecord update(@PathVariable long id, @RequestBody EmployeeRequest r) { return service.update(id, r); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
