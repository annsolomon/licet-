package com.college.repository;

import com.college.model.Faculty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** SQL for table FACULTY (joined with DEPARTMENT for the code). */
@Repository
public class FacultyRepository {

    private static final String SELECT = """
            SELECT f.faculty_id, f.faculty_code, f.faculty_name, f.email, f.designation, f.dept_id, d.dept_code
              FROM faculty f JOIN department d ON d.dept_id = f.dept_id
            """;

    private static final RowMapper<Faculty> MAPPER = (rs, i) -> new Faculty(
            rs.getLong("faculty_id"), rs.getString("faculty_code"), rs.getString("faculty_name"),
            rs.getString("email"), rs.getString("designation"), rs.getLong("dept_id"), rs.getString("dept_code"));

    private final JdbcTemplate jdbc;

    public FacultyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Faculty> findAll() {
        return jdbc.query(SELECT + " ORDER BY f.faculty_id", MAPPER);
    }

    public Optional<Faculty> findById(long id) {
        return jdbc.query(SELECT + " WHERE f.faculty_id = ?", MAPPER, id).stream().findFirst();
    }

    public long nextId() {
        Long id = jdbc.queryForObject("SELECT seq_faculty.NEXTVAL FROM dual", Long.class);
        return id == null ? 0 : id;
    }

    public int insert(long id, String code, String name, String email, String designation, long deptId) {
        return jdbc.update("""
                INSERT INTO faculty (faculty_id, faculty_code, faculty_name, email, designation, dept_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, code, name, email, designation, deptId);
    }

    public int update(long id, String code, String name, String email, String designation, long deptId) {
        return jdbc.update("""
                UPDATE faculty SET faculty_code = ?, faculty_name = ?, email = ?, designation = ?, dept_id = ?
                 WHERE faculty_id = ?
                """, code, name, email, designation, deptId, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM faculty WHERE faculty_id = ?", id);
    }
}
