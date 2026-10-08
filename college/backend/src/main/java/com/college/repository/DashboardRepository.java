package com.college.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/** Count queries for the dashboard cards and charts. */
@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** SELECT COUNT(*) FROM <table>. The table name is chosen by our own code (never by the user). */
    public long count(String table) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return n == null ? 0 : n;
    }

    public Map<String, Long> studentsByDepartment() {
        Map<String, Long> map = new LinkedHashMap<>();
        jdbc.query("""
                SELECT d.dept_code, COUNT(s.student_id) AS n
                  FROM department d LEFT JOIN student s ON s.dept_id = d.dept_id
                 GROUP BY d.dept_id, d.dept_code ORDER BY d.dept_id
                """, rs -> {
            map.put(rs.getString("dept_code"), rs.getLong("n"));
        });
        return map;
    }
}
