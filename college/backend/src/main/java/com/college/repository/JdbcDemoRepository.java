package com.college.repository;

import com.college.exception.DatabaseException;
import com.college.model.Student;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SYLLABUS: JDBC - the SAME query written two ways, side by side.
 *
 *   PLAIN JDBC                                   JdbcTemplate
 *   1. Driver (ojdbc11.jar on the classpath)     (same driver underneath)
 *   2. Connection  = DataSource.getConnection()  (Spring gets / returns the connection)
 *   3. PreparedStatement = con.prepareStatement(sql)   (Spring creates it)
 *   4. ps.setLong(1, deptId)  bind the ?         (Spring binds the varargs)
 *   5. ResultSet = ps.executeQuery()             (Spring executes)
 *   6. while (rs.next()) { ... new Student ... } (RowMapper does the loop)
 *   7. close ResultSet, Statement, Connection    (Spring always closes)
 *   8. catch SQLException                        (Spring converts to DataAccessException)
 */
@Repository
public class JdbcDemoRepository {

    private static final String SQL = """
            SELECT s.student_id, s.student_name, s.email, s.phone, s.dept_id, d.dept_code,
                   s.class_id, c.class_name, s.joined_year
              FROM student s
              JOIN department d ON d.dept_id = s.dept_id
              JOIN class c ON c.class_id = s.class_id
             WHERE s.dept_id = ?
             ORDER BY s.student_id
            """;

    private final DataSource dataSource;
    private final StudentRepository studentRepository;

    public JdbcDemoRepository(DataSource dataSource, StudentRepository studentRepository) {
        this.dataSource = dataSource;
        this.studentRepository = studentRepository;
    }

    /** The long way: every JDBC step written by hand. */
    public List<Student> findByDepartmentPlainJdbc(long deptId) {
        List<Student> students = new ArrayList<>();
        // try-with-resources closes Connection, PreparedStatement and ResultSet automatically
        try (Connection con = dataSource.getConnection();                  // step 2: Connection
             PreparedStatement ps = con.prepareStatement(SQL)) {           // step 3: PreparedStatement
            ps.setLong(1, deptId);                                         // step 4: bind the ? (no string concatenation!)
            try (ResultSet rs = ps.executeQuery()) {                       // step 5: execute -> ResultSet
                while (rs.next()) {                                        // step 6: loop over rows
                    int year = rs.getInt("joined_year");
                    Integer joinedYear = rs.wasNull() ? null : year;       // wasNull() refers to the LAST column read
                    students.add(new Student(
                            rs.getLong("student_id"), rs.getString("student_name"), rs.getString("email"),
                            rs.getString("phone"), rs.getLong("dept_id"), rs.getString("dept_code"),
                            rs.getLong("class_id"), rs.getString("class_name"), joinedYear));
                }
            }
        } catch (SQLException e) {                                         // step 8: checked exception
            throw new DatabaseException("Plain JDBC query failed: " + e.getMessage(), e);
        }
        return students;
    }

    /** The short way: one line. (StudentRepository uses JdbcTemplate.query + RowMapper.) */
    public List<Student> findByDepartmentJdbcTemplate(long deptId) {
        return studentRepository.findByDepartment(deptId);
    }

    public String sql() {
        return SQL;
    }
}
