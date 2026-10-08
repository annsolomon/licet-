package com.college.config;

import com.college.exception.ForbiddenException;
import com.college.exception.UnauthorizedException;
import com.college.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Checks the token on every /api request (except login and health).
 *
 *   ADMIN   : everything
 *   FACULTY : may READ everything except employees / payroll / audit,
 *             and may WRITE attendance, marks, results, reports and the two labs
 *
 * Exceptions thrown here are turned into HTTP 401 / 403 by GlobalExceptionHandler.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER = "college.user";

    private static final List<String> ADMIN_ONLY_PREFIXES = List.of("/api/employees", "/api/payroll", "/api/audit");
    private static final List<String> FACULTY_WRITE_PREFIXES =
            List.of("/api/attendance", "/api/marks", "/api/results", "/api/reports", "/api/lab", "/api/dblab", "/api/auth");

    private final AuthService auth;

    public AuthInterceptor(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;                               // CORS pre-flight
        }
        String path = request.getRequestURI();
        if (path.equals("/api/auth/login") || path.equals("/api/health")) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
        AuthService.Session session = auth.find(token)
                .orElseThrow(() -> new UnauthorizedException("Please log in first (missing or expired token)"));
        request.setAttribute(ATTR_USER, session);

        boolean admin = "ADMIN".equals(session.role());
        if (!admin) {
            if (ADMIN_ONLY_PREFIXES.stream().anyMatch(path::startsWith)
                    || path.startsWith("/api/reports/files") && path.contains("payroll")) {
                throw new ForbiddenException("Only an ADMIN may open " + path);
            }
            boolean read = "GET".equalsIgnoreCase(request.getMethod());
            if (!read && FACULTY_WRITE_PREFIXES.stream().noneMatch(path::startsWith)) {
                throw new ForbiddenException("Role " + session.role() + " may not change " + path);
            }
        }
        return true;
    }
}
