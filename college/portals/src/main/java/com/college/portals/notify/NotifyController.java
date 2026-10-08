package com.college.portals.notify;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API of the notification service (login as admin / admin123 on POST /api/login).
 *   GET  /api/stats                counts per recipient and status
 *   GET  /api/recent?status=FAILED the latest notifications
 *   POST /api/dispatch             send the pending ones now
 *   POST /api/retry                put FAILED ones back to PENDING
 */
@RestController
@RequestMapping("/api")
@Profile("notify")
public class NotifyController {

    private final NotificationRepository repository;
    private final DispatchService service;

    public NotifyController(NotificationRepository repository, DispatchService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping("/stats")
    public List<Map<String, Object>> stats() {
        return repository.stats();
    }

    @GetMapping("/recent")
    public List<Map<String, Object>> recent(@RequestParam(required = false) String status, @RequestParam(defaultValue = "50") int limit) {
        return repository.recent(status == null || status.isBlank() ? "ALL" : status.toUpperCase(), Math.max(1, Math.min(limit, 500)));
    }

    @PostMapping("/dispatch")
    public Map<String, Integer> dispatch() {
        return service.dispatch();
    }

    @PostMapping("/retry")
    public Map<String, Object> retry() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("requeued", service.retryFailed());
        m.putAll(service.dispatch());
        return m;
    }
}
