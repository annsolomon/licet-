package com.college.controller;

import com.college.config.AuthInterceptor;
import com.college.dto.LoginRequest;
import com.college.dto.LoginResponse;
import com.college.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** POST /api/auth/login , POST /api/auth/logout , GET /api/auth/me */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        auth.logout(header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null);
        return Map.of("message", "Logged out");
    }

    @GetMapping("/me")
    public AuthService.Session me(HttpServletRequest request) {
        return (AuthService.Session) request.getAttribute(AuthInterceptor.ATTR_USER);
    }
}
