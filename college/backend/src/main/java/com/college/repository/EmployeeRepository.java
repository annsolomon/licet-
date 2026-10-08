package com.college.repository;

import com.college.model.EmployeeRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** SQL for table EMPLOYEE. */
@Repository
public class EmployeeRepository {

    private static final String SELECT = "SELECT emp_id, emp_name, email, designation, basic_salary FROM employee";

    private static final RowMapper<EmployeeRecord> MAPPER = (rs, i) -> new EmployeeRecord(
            rs.getLong("emp_id"), rs.getString("emp_name"), rs.getString("email"),
            rs.getString("designation"), rs.getBigDecimal("basic_salary"));

    private final JdbcTemplate jdbc;

    public EmployeeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EmployeeRecord> findAll() {
        return jdbc.query(SELECT + " ORDER BY emp_id", MAPPER);
    }

    public Optional<EmployeeRecord> findById(long id) {
        return jdbc.query(SELECT + " WHERE emp_id = ?", MAPPER, id).stream().findFirst();
    }

    public long nextId() {
        Long id = jdbc.queryForObject("SELECT seq_employee.NEXTVAL FROM dual", Long.class);
        return id == null ? 0 : id;
    }

    public int insert(long id, String name, String email, String designation, BigDecimal basic) {
        return jdbc.update("INSERT INTO employee (emp_id, emp_name, email, designation, basic_salary) VALUES (?, ?, ?, ?, ?)",
                id, name, email, designation, basic);
    }

    public int update(long id, String name, String email, String designation, BigDecimal basic) {
        return jdbc.update("UPDATE employee SET emp_name = ?, email = ?, designation = ?, basic_salary = ? WHERE emp_id = ?",
                name, email, designation, basic, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM employee WHERE emp_id = ?", id);
    }
}
