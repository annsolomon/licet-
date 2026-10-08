package com.college.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** GET /api/health - public. Says whether Spring is up and whether Oracle answers. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("api", "UP");
        try {
            Integer one = jdbc.queryForObject("SELECT 1 FROM dual", Integer.class);
            Integer tables = jdbc.queryForObject("SELECT COUNT(*) FROM user_tables", Integer.class);
            m.put("database", one != null && one == 1 ? "UP" : "DOWN");
            m.put("tables", tables);
        } catch (Exception e) {
            m.put("database", "DOWN");
            m.put("detail", "Oracle is not reachable: " + e.getClass().getSimpleName());
        }
        return m;
    }
}
