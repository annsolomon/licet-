package com.college.repository;

import com.college.model.ClassSection;
import com.college.model.ClassSubject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** SQL for tables CLASS and CLASS_SUBJECT (which faculty teaches which subject to which class). */
@Repository
public class ClassRepository {

    private static final String SELECT = """
            SELECT c.class_id, c.class_name, c.dept_id, d.dept_code, c.semester, c.section_name, c.academic_year,
                   (SELECT COUNT(*) FROM student s WHERE s.class_id = c.class_id) AS student_count
              FROM class c JOIN department d ON d.dept_id = c.dept_id
            """;

    private static final RowMapper<ClassSection> MAPPER = (rs, i) -> new ClassSection(
            rs.getLong("class_id"), rs.getString("class_name"), rs.getLong("dept_id"), rs.getString("dept_code"),
            rs.getInt("semester"), rs.getString("section_name"), rs.getString("academic_year"),
            rs.getInt("student_count"));

    private static final String SELECT_CS = """
            SELECT cs.cs_id, cs.class_id, c.class_name, cs.subject_id, sb.subject_code, sb.subject_name,
                   cs.faculty_id, f.faculty_name
              FROM class_subject cs
              JOIN class   c  ON c.class_id    = cs.class_id
              JOIN subject sb ON sb.subject_id = cs.subject_id
              JOIN faculty f  ON f.faculty_id  = cs.faculty_id
            """;

    private static final RowMapper<ClassSubject> CS_MAPPER = (rs, i) -> new ClassSubject(
            rs.getLong("cs_id"), rs.getLong("class_id"), rs.getString("class_name"),
            rs.getLong("subject_id"), rs.getString("subject_code"), rs.getString("subject_name"),
            rs.getLong("faculty_id"), rs.getString("faculty_name"));

    private final JdbcTemplate jdbc;

    public ClassRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ClassSection> findAll() {
        return jdbc.query(SELECT + " ORDER BY c.class_id", MAPPER);
    }

    public Optional<ClassSection> findById(long id) {
        return jdbc.query(SELECT + " WHERE c.class_id = ?", MAPPER, id).stream().findFirst();
    }

    public long nextId() {
        Long id = jdbc.queryForObject("SELECT seq_class.NEXTVAL FROM dual", Long.class);
        return id == null ? 0 : id;
    }

    public int insert(long id, String name, long deptId, int semester, String section, String year) {
        return jdbc.update("""
                INSERT INTO class (class_id, class_name, dept_id, semester, section_name, academic_year)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, name, deptId, semester, section, year);
    }

    public int update(long id, String name, long deptId, int semester, String section, String year) {
        return jdbc.update("""
                UPDATE class SET class_name = ?, dept_id = ?, semester = ?, section_name = ?, academic_year = ?
                 WHERE class_id = ?
                """, name, deptId, semester, section, year, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM class WHERE class_id = ?", id);
    }

    // ---------- CLASS_SUBJECT ----------
    public List<ClassSubject> findAllAssignments() {
        return jdbc.query(SELECT_CS + " ORDER BY cs.class_id, sb.subject_code", CS_MAPPER);
    }

    public List<ClassSubject> findAssignmentsByClass(long classId) {
        return jdbc.query(SELECT_CS + " WHERE cs.class_id = ? ORDER BY sb.subject_code", CS_MAPPER, classId);
    }

    public Optional<ClassSubject> findAssignment(long classId, long subjectId) {
        return jdbc.query(SELECT_CS + " WHERE cs.class_id = ? AND cs.subject_id = ?", CS_MAPPER, classId, subjectId)
                .stream().findFirst();
    }

    public long nextAssignmentId() {
        Long id = jdbc.queryForObject("SELECT seq_class_subject.NEXTVAL FROM dual", Long.class);
        return id == null ? 0 : id;
    }

    public int insertAssignment(long id, long classId, long subjectId, long facultyId) {
        return jdbc.update("INSERT INTO class_subject (cs_id, class_id, subject_id, faculty_id) VALUES (?, ?, ?, ?)",
                id, classId, subjectId, facultyId);
    }

    public int deleteAssignment(long id) {
        return jdbc.update("DELETE FROM class_subject WHERE cs_id = ?", id);
    }
}
