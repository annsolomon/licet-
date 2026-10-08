package com.college.controller;

import com.college.dto.ReportRequest;
import com.college.service.ReportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Reports: generate (exports a .txt file), several at once (threads), list, view, download, copy, count a character. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/types")
    public List<String> types() { return service.types(); }

    @PostMapping
    public ReportService.ReportFile generate(@RequestBody ReportRequest r) { return service.generate(r); }

    @PostMapping("/parallel")
    public List<ReportService.ReportFile> parallel(@RequestBody ReportRequest.Batch b) {
        return service.generateParallel(b.types());
    }

    @GetMapping("/files")
    public List<Map<String, Object>> files() { return service.listFiles(); }

    @GetMapping("/files/{name}")
    public List<String> read(@PathVariable String name) { return service.read(name); }

    @GetMapping("/files/{name}/download")
    public ResponseEntity<Resource> download(@PathVariable String name) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(new FileSystemResource(service.fileForDownload(name)));
    }

    @GetMapping("/files/{name}/count")
    public Map<String, Object> count(@PathVariable String name, @RequestParam String character) {
        return service.countCharacter(name, character);
    }

    @PostMapping("/files/{name}/copy")
    public Map<String, Object> copy(@PathVariable String name) { return service.copy(name); }
}
