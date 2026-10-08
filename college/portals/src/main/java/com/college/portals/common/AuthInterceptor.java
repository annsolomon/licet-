package com.college.portals.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Every /api call except login and health needs "Authorization: Bearer <token>" of THIS portal. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR = "portal.session";
    private final AuthService auth;

    public AuthInterceptor(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || path.equals("/api/login") || path.equals("/api/health")) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
        Session s = auth.find(token).orElseThrow(() -> ApiException.unauthorized("Please log in"));
        request.setAttribute(ATTR, s);
        return true;
    }

    public static Session session(HttpServletRequest request) {
        return (Session) request.getAttribute(ATTR);
    }
}
