package com.college.portals.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** GET /api/notifications, GET /api/notifications/unread-count, POST /api/notifications/{id}/read, POST /api/notifications/read-all */
@RestController
@RequestMapping("/api/notifications")
@Profile({"parent", "advisor"})
public class InboxController {

    private final InboxRepository repository;

    public InboxController(InboxRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Map<String, Object>> list(HttpServletRequest request, @RequestParam(defaultValue = "false") boolean unread,
                                          @RequestParam(defaultValue = "60") int limit) {
        Session s = AuthInterceptor.session(request);
        return repository.list(s.role(), s.linkId(), unread, Math.max(1, Math.min(limit, 200)));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(HttpServletRequest request) {
        Session s = AuthInterceptor.session(request);
        return Map.of("unread", repository.unread(s.role(), s.linkId()));
    }

    @PostMapping("/{id}/read")
    public Map<String, Integer> read(HttpServletRequest request, @PathVariable long id) {
        Session s = AuthInterceptor.session(request);
        return Map.of("updated", repository.markRead(s.role(), s.linkId(), id));
    }

    @PostMapping("/read-all")
    public Map<String, Integer> readAll(HttpServletRequest request) {
        Session s = AuthInterceptor.session(request);
        return Map.of("updated", repository.markAllRead(s.role(), s.linkId()));
    }
}
