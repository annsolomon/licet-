package com.college.repository;

import com.college.model.Department;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** SQL for table DEPARTMENT. studentCount comes from a LEFT JOIN + GROUP BY (departments without students show 0). */
@Repository
public class DepartmentRepository {

    private static final String SELECT = """
            SELECT d.dept_id, d.dept_code, d.dept_name, COUNT(s.student_id) AS student_count
              FROM department d
              LEFT JOIN student s ON s.dept_id = d.dept_id
            """;
    private static final String GROUP = " GROUP BY d.dept_id, d.dept_code, d.dept_name ";

    private static final RowMapper<Department> MAPPER = (rs, i) -> new Department(
            rs.getLong("dept_id"), rs.getString("dept_code"), rs.getString("dept_name"), rs.getInt("student_count"));

    private final JdbcTemplate jdbc;

    public DepartmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Department> findAll() {
        return jdbc.query(SELECT + GROUP + " ORDER BY d.dept_id", MAPPER);
    }

    public Optional<Department> findById(long id) {
        return jdbc.query(SELECT + " WHERE d.dept_id = ? " + GROUP, MAPPER, id).stream().findFirst();
    }

    public long nextId() {
        Long id = jdbc.queryForObject("SELECT seq_department.NEXTVAL FROM dual", Long.class);
        return id == null ? 0 : id;
    }

    public int insert(long id, String code, String name) {
        return jdbc.update("INSERT INTO department (dept_id, dept_code, dept_name) VALUES (?, ?, ?)", id, code, name);
    }

    public int update(long id, String code, String name) {
        return jdbc.update("UPDATE department SET dept_code = ?, dept_name = ? WHERE dept_id = ?", code, name, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM department WHERE dept_id = ?", id);
    }
}
