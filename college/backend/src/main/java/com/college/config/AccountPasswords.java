package com.college.config;

import com.college.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Public deployment: replaces the well-known demo passwords (admin123 / faculty123) with the values of
 * the ADMIN_PASSWORD and FACULTY_PASSWORD environment variables at start-up.
 * With app.require-custom-passwords=true (prod profile) the app refuses to start without them.
 */
@Component
public class AccountPasswords implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AccountPasswords.class);

    private final JdbcTemplate jdbc;
    private final String adminPassword;
    private final String facultyPassword;
    private final boolean required;

    public AccountPasswords(JdbcTemplate jdbc,
                            @Value("${app.admin-password:}") String adminPassword,
                            @Value("${app.faculty-password:}") String facultyPassword,
                            @Value("${app.require-custom-passwords:false}") boolean required) {
        this.jdbc = jdbc;
        this.adminPassword = adminPassword;
        this.facultyPassword = facultyPassword;
        this.required = required;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (required) {
            check("ADMIN_PASSWORD", adminPassword);
            check("FACULTY_PASSWORD", facultyPassword);
        }
        set("admin", adminPassword);
        set("faculty", facultyPassword);
    }

    private static void check(String name, String value) {
        if (value == null || value.length() < 12) {
            throw new IllegalStateException(name + " must be set to at least 12 characters for a public deployment");
        }
    }

    private void set(String username, String password) {
        if (password == null || password.isBlank()) {
            return;
        }
        try {
            int n = jdbc.update("UPDATE app_user SET password_hash = ? WHERE username = ?", PasswordUtil.hash(password), username);
            LOG.info("Password of '{}' replaced from environment ({} row)", username, n);
        } catch (Exception e) {
            LOG.error("Could not set the password of '{}' (database not reachable?): {}", username, e.getMessage());
        }
    }
}
