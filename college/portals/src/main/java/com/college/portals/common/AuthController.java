package com.college.portals.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** POST /api/login  POST /api/logout  GET /api/me  GET /api/health  (the same in every portal) */
@RestController
@RequestMapping("/api")
public class AuthController {

    public record LoginRequest(String username, String password) { }

    private final AuthService auth;
    private final JdbcTemplate jdbc;
    private final String portal;

    public AuthController(AuthService auth, JdbcTemplate jdbc, @Value("${portal.name}") String portal) {
        this.auth = auth;
        this.jdbc = jdbc;
        this.portal = portal;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest r) {
        return auth.login(r.username(), r.password());
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        auth.logout(header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null);
        return Map.of("message", "Logged out");
    }

    @GetMapping("/me")
    public Session me(HttpServletRequest request) {
        return AuthInterceptor.session(request);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("service", portal);
        m.put("status", "UP");
        try {
            jdbc.queryForObject("SELECT 1 FROM dual", Integer.class);
            m.put("database", "UP");
        } catch (Exception e) {
            m.put("database", "DOWN");
        }
        return m;
    }
}
