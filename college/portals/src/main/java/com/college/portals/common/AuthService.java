package com.college.portals.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * Login for ONE portal. The portal's role comes from application-<profile>.properties (portal.role),
 * so the parent portal only accepts PARENT accounts, the HOD portal only HOD accounts and so on.
 */
@Service
public class AuthService {

    private record UserRow(String username, String hash, String fullName, String role, long facultyId, long parentId, long studentId) { }

    private final JdbcTemplate jdbc;
    private final String portalRole;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(JdbcTemplate jdbc, @Value("${portal.role}") String portalRole) {
        this.jdbc = jdbc;
        this.portalRole = portalRole;
    }

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw ApiException.unauthorized("Enter username and password");
        }
        List<UserRow> found = jdbc.query("""
                SELECT username, password_hash, full_name, role_name, NVL(faculty_id, 0) AS fid, NVL(parent_id, 0) AS pid, NVL(student_id, 0) AS sid
                  FROM app_user WHERE username = ?
                """, (rs, i) -> new UserRow(rs.getString("username"), rs.getString("password_hash"),
                rs.getString("full_name"), rs.getString("role_name"), rs.getLong("fid"), rs.getLong("pid"), rs.getLong("sid")), username.trim());
        if (found.isEmpty() || !found.get(0).hash().equals(PasswordUtil.hash(password))) {
            throw ApiException.unauthorized("Wrong username or password");
        }
        UserRow u = found.get(0);
        if (!portalRole.equals(u.role())) {
            throw ApiException.forbidden("This is the " + portalRole + " portal. Your account is a " + u.role() + " account.");
        }
        long link = "PARENT".equals(u.role()) ? u.parentId() : "STUDENT".equals(u.role()) ? u.studentId() : u.facultyId();
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(u.username(), u.fullName(), u.role(), link));
        return Map.of("token", token, "username", u.username(), "fullName", u.fullName(), "role", u.role());
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
