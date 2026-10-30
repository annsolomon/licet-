package com.college.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs before every request (public deployment hardening):
 *   1. security headers on every response
 *   2. demo expiry: after app.expires-at every /api call except /api/health answers 410 Gone
 *   3. login brute-force limit: at most N login attempts per IP per 15 minutes (429)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)   // right after the CORS filter
public class SecurityFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final Instant expiresAt;
    private final int maxLoginAttempts;
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public SecurityFilter(@Value("${app.expires-at:}") String expiresAt,
                          @Value("${app.login-attempts-per-15min:10}") int maxLoginAttempts) {
        this.expiresAt = expiresAt == null || expiresAt.isBlank() ? null : Instant.parse(expiresAt.trim());
        this.maxLoginAttempts = maxLoginAttempts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "no-referrer");
        res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        res.setHeader("Cache-Control", "no-store");
        if (req.isSecure()) {
            res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        String path = req.getRequestURI();
        boolean preflight = "OPTIONS".equalsIgnoreCase(req.getMethod());

        if (!preflight && expiresAt != null && Instant.now().isAfter(expiresAt) && !path.equals("/api/health")) {
            reject(res, 410, "DemoEnded", "This demo deployment has ended.");
            return;
        }

        if (!preflight && path.equals("/api/auth/login") && tooManyAttempts(req.getRemoteAddr())) {
            res.setHeader("Retry-After", "900");
            reject(res, 429, "TooManyRequests", "Too many login attempts. Try again in 15 minutes.");
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean tooManyAttempts(String ip) {
        long now = System.currentTimeMillis();
        if (attempts.size() > 10_000) {
            attempts.clear();                                   // memory guard against address flooding
        }
        Deque<Long> q = attempts.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) {
                q.pollFirst();
            }
            if (q.size() >= maxLoginAttempts) {
                return true;
            }
            q.addLast(now);
            return false;
        }
    }

    private static void reject(HttpServletResponse res, int status, String error, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + message
                + "\",\"layer\":\"Security filter\"}");
    }
}
