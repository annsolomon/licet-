package com.college.controller;

import com.college.dto.DemoStepResult;
import com.college.dto.DemoTopic;
import com.college.service.DbmsDemoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** DBMS LAB: list the 32 topics and run one (all demos roll back at the end). */
@RestController
@RequestMapping("/api/dblab")
public class DbLabController {

    private final DbmsDemoService service;

    public DbLabController(DbmsDemoService service) {
        this.service = service;
    }

    @GetMapping("/topics")
    public List<DemoTopic> topics() { return service.catalog(); }

    @GetMapping("/topics/{number}")
    public DemoTopic topic(@PathVariable int number) { return service.topic(number); }

    @PostMapping("/topics/{number}/run")
    public List<DemoStepResult> run(@PathVariable int number) { return service.run(number); }
}
