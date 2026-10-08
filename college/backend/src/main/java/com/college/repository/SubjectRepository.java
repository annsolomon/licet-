package com.college.repository;

import com.college.model.Subject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** SQL for table SUBJECT. */
@Repository
public class SubjectRepository {

    private static final String SELECT = """
            SELECT sb.subject_id, sb.subject_code, sb.subject_name, sb.credits, sb.semester, sb.dept_id, d.dept_code
              FROM subject sb JOIN department d ON d.dept_id = sb.dept_id
            """;

    private static final RowMapper<Subject> MAPPER = (rs, i) -> new Subject(
            rs.getLong("subject_id"), rs.getString("subject_code"), rs.getString("subject_name"),
            rs.getInt("credits"), rs.getInt("semester"), rs.getLong("dept_id"), rs.getString("dept_code"));

    private final JdbcTemplate jdbc;

    public SubjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Subject> findAll() {
        return jdbc.query(SELECT + " ORDER BY sb.semester, sb.subject_code", MAPPER);
    }

    public List<Subject> findByDepartment(long deptId) {
        return jdbc.query(SELECT + " WHERE sb.dept_id = ? ORDER BY sb.semester, sb.subject_code", MAPPER, deptId);
    }

    public Optional<Subject> findById(long id) {
        return jdbc.query(SELECT + " WHERE sb.subject_id = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<Subject> findByCode(String code) {
        return jdbc.query(SELECT + " WHERE sb.subject_code = ?", MAPPER, code).stream().findFirst();
    }

    public long nextId() {
        Long id = jdbc.queryForObject("SELECT seq_subject.NEXTVAL FROM dual", Long.class);
        return id == null ? 0 : id;
    }

    public int insert(long id, String code, String name, int credits, int semester, long deptId) {
        return jdbc.update("""
                INSERT INTO subject (subject_id, subject_code, subject_name, credits, semester, dept_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, code, name, credits, semester, deptId);
    }

    public int update(long id, String code, String name, int credits, int semester, long deptId) {
        return jdbc.update("""
                UPDATE subject SET subject_code = ?, subject_name = ?, credits = ?, semester = ?, dept_id = ?
                 WHERE subject_id = ?
                """, code, name, credits, semester, deptId, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM subject WHERE subject_id = ?", id);
    }
}
