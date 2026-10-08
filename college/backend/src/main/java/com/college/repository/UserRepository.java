package com.college.repository;

import com.college.model.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Login accounts (table APP_USER). */
@Repository
public class UserRepository {

    private static final RowMapper<AppUser> MAPPER = (rs, i) -> new AppUser(
            rs.getLong("user_id"), rs.getString("username"), rs.getString("password_hash"),
            rs.getString("full_name"), rs.getString("role_name"));

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByUsername(String username) {
        return jdbc.query("SELECT user_id, username, password_hash, full_name, role_name FROM app_user WHERE username = ?",
                MAPPER, username).stream().findFirst();
    }
}
