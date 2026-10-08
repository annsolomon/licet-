package com.college.controller;

import com.college.dto.PayrollRequest;
import com.college.model.PayrollRecord;
import com.college.service.PayrollService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Payroll: generate with Java OOP (/generate) or with the PL/SQL procedure (/generate-plsql). */
@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService service;

    public PayrollController(PayrollService service) {
        this.service = service;
    }

    @GetMapping
    public List<PayrollRecord> list(@RequestParam(required = false) String month) { return service.list(month); }

    @GetMapping("/{id}")
    public PayrollRecord get(@PathVariable long id) { return service.get(id); }

    @GetMapping("/{id}/payslip")
    public Map<String, Object> payslip(@PathVariable long id) {
        return Map.of("payroll", service.get(id), "lines", service.payslipLines(id));
    }

    @PostMapping("/generate")
    public PayrollRecord generate(@RequestBody PayrollRequest r) { return service.generateJava(r); }

    @PostMapping("/generate-all")
    public Map<String, Object> generateAll(@RequestBody PayrollRequest r) { return service.generateAllJava(r.month()); }

    @PostMapping("/generate-plsql")
    public PayrollRecord generatePlsql(@RequestBody PayrollRequest r) { return service.generatePlsql(r); }

    @PostMapping("/generate-all-plsql")
    public Map<String, Object> generateAllPlsql(@RequestBody PayrollRequest r) { return service.generateAllPlsql(r.month()); }

    @DeleteMapping
    public Map<String, Object> deleteMonth(@RequestParam String month) {
        return Map.of("month", month, "deleted", service.deleteMonth(month));
    }
}
