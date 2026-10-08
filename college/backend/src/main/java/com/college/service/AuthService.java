package com.college.service;

import com.college.dto.LoginRequest;
import com.college.dto.LoginResponse;
import com.college.exception.UnauthorizedException;
import com.college.model.AppUser;
import com.college.repository.UserRepository;
import com.college.util.PasswordUtil;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LOGIN.
 *   React login form -> POST /api/auth/login -> AuthController -> AuthService
 *   -> UserRepository (SELECT ... FROM app_user WHERE username = ?) -> compare SHA-256 hashes
 *   -> random token (UUID) kept in memory -> JSON {token, role} -> React stores it
 *   -> every later request carries   Authorization: Bearer <token>   (checked by AuthInterceptor)
 *
 * @Service = a Spring "bean" holding business logic (the middle layer between controller and repository).
 */
@Service
public class AuthService {

    /** Who a token belongs to. */
    public record Session(String username, String fullName, String role) { }

    private final UserRepository users;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();   // thread-safe map: token -> session

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isEmpty()) {
            throw new UnauthorizedException("Username and password are required");
        }
        AppUser user = users.findByUsername(request.username().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (!user.passwordHash().equals(PasswordUtil.hash(request.password()))) {
            throw new UnauthorizedException("Invalid username or password");
        }
        if (!"ADMIN".equals(user.role()) && !"FACULTY".equals(user.role())) {
            throw new UnauthorizedException("HOD, advisor and parent accounts sign in to their own portal, not here");
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(user.username(), user.fullName(), user.role()));
        return new LoginResponse(token, user.username(), user.fullName(), user.role());
    }

    public Optional<Session> find(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(sessions.get(token));
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
