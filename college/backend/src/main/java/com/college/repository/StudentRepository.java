package com.college.repository;

import com.college.model.Student;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORY (DAO) = the ONLY layer that talks to Oracle for students.
 *
 *   StudentService --calls--> StudentRepository --uses--> JdbcTemplate --sends--> raw SQL --> Oracle
 *
 * @Repository : tells Spring "create ONE object of this class and keep it" (a "bean") and
 *               translate Oracle errors into Spring's DataAccessException family
 *               (ORA-00001 -> DuplicateKeyException, ORA-02291/02292 -> DataIntegrityViolationException).
 *
 * JdbcTemplate: Spring's helper that does the boring JDBC work for us
 *               (get connection, PreparedStatement, bind ?, execute, loop the ResultSet, close everything).
 */
@Repository
public class StudentRepository {

    /** Base SELECT shared by all finders (joins STUDENT -> DEPARTMENT and STUDENT -> CLASS). */
    private static final String SELECT_BASE = """
            SELECT s.student_id, s.student_name, s.email, s.phone,
                   s.dept_id, d.dept_code, s.class_id, c.class_name, s.joined_year
              FROM student s
              JOIN department d ON d.dept_id  = s.dept_id
              JOIN class      c ON c.class_id = s.class_id
            """;

    /**
     * RowMapper = "how to turn ONE ResultSet row into ONE Java object".
     *   ResultSet row (STUDENT_ID=101, STUDENT_NAME=Rahul ...)  ->  new Student(101, "Rahul", ...)
     */
    private static final RowMapper<Student> MAPPER = (rs, rowNum) -> new Student(
            rs.getLong("student_id"),
            rs.getString("student_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getLong("dept_id"),
            rs.getString("dept_code"),
            rs.getLong("class_id"),
            rs.getString("class_name"),
            RsUtil.integer(rs, "joined_year"));

    private final JdbcTemplate jdbc;

    /** Constructor injection (Dependency Injection): Spring passes the ready JdbcTemplate in. */
    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Student> findAll() {
        return jdbc.query(SELECT_BASE + " ORDER BY s.student_id", MAPPER);
    }

    public Optional<Student> findById(long id) {
        List<Student> rows = jdbc.query(SELECT_BASE + " WHERE s.student_id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public List<Student> findByDepartment(long deptId) {
        return jdbc.query(SELECT_BASE + " WHERE s.dept_id = ? ORDER BY s.student_id", MAPPER, deptId);
    }

    public List<Student> findByClass(long classId) {
        return jdbc.query(SELECT_BASE + " WHERE s.class_id = ? ORDER BY s.student_id", MAPPER, classId);
    }

    /** SQL search: parameters (?) are bound by the driver - user text is NEVER concatenated into the SQL. */
    public List<Student> searchBySql(String keyword) {
        String like = "%" + keyword.trim().toUpperCase() + "%";
        return jdbc.query(SELECT_BASE
                        + " WHERE UPPER(s.student_name) LIKE ? OR TO_CHAR(s.student_id) LIKE ? ORDER BY s.student_id",
                MAPPER, like, like);
    }

    public boolean existsById(long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM student WHERE student_id = ?", Integer.class, id);
        return n != null && n > 0;
    }

    public boolean existsByEmail(String email) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM student WHERE LOWER(email) = LOWER(?)", Integer.class, email);
        return n != null && n > 0;
    }

    /** Asks the Oracle SEQUENCE for the next free student id. */
    public long nextId() {
        Long id = jdbc.queryForObject("SELECT seq_student.NEXTVAL FROM dual", Long.class);
        return id == null ? 0 : id;
    }

    /** INSERT. update() returns the number of rows changed (here 1). The Oracle INSERT trigger writes AUDIT_LOG. */
    public int insert(long id, String name, String email, String phone, long deptId, long classId, Integer joinedYear) {
        return jdbc.update("""
                INSERT INTO student (student_id, student_name, email, phone, dept_id, class_id, joined_year)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, name, email, phone, deptId, classId, joinedYear);
    }

    public int update(long id, String name, String email, String phone, long deptId, long classId, Integer joinedYear) {
        return jdbc.update("""
                UPDATE student
                   SET student_name = ?, email = ?, phone = ?, dept_id = ?, class_id = ?, joined_year = ?
                 WHERE student_id = ?
                """, name, email, phone, deptId, classId, joinedYear, id);
    }

    /** DELETE. The Oracle DELETE trigger writes AUDIT_LOG; child rows (marks ...) block it (ORA-02292). */
    public int delete(long id) {
        return jdbc.update("DELETE FROM student WHERE student_id = ?", id);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM student", Long.class);
        return n == null ? 0 : n;
    }
}
