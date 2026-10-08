package com.college.repository;

import com.college.model.PayrollRecord;
import com.college.payroll.Payslip;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** SQL for table PAYROLL (one row per employee per month, UNIQUE(emp_id, pay_month)). */
@Repository
public class PayrollRepository {

    private static final String SELECT = """
            SELECT p.payroll_id, p.emp_id, e.emp_name, e.designation, p.pay_month,
                   p.basic, p.hra, p.da, p.pf, p.gross, p.net
              FROM payroll p JOIN employee e ON e.emp_id = p.emp_id
            """;

    private static final RowMapper<PayrollRecord> MAPPER = (rs, i) -> new PayrollRecord(
            rs.getLong("payroll_id"), rs.getLong("emp_id"), rs.getString("emp_name"), rs.getString("designation"),
            rs.getString("pay_month"), rs.getBigDecimal("basic"), rs.getBigDecimal("hra"), rs.getBigDecimal("da"),
            rs.getBigDecimal("pf"), rs.getBigDecimal("gross"), rs.getBigDecimal("net"));

    private final JdbcTemplate jdbc;

    public PayrollRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PayrollRecord> findAll() {
        return jdbc.query(SELECT + " ORDER BY p.pay_month DESC, p.emp_id", MAPPER);
    }

    public List<PayrollRecord> findByMonth(String month) {
        return jdbc.query(SELECT + " WHERE p.pay_month = ? ORDER BY p.emp_id", MAPPER, month);
    }

    public List<PayrollRecord> findByEmployee(long empId) {
        return jdbc.query(SELECT + " WHERE p.emp_id = ? ORDER BY p.pay_month DESC", MAPPER, empId);
    }

    public Optional<PayrollRecord> findById(long id) {
        return jdbc.query(SELECT + " WHERE p.payroll_id = ?", MAPPER, id).stream().findFirst();
    }

    /** The Java (OOP) path: the amounts were calculated by Employee subclasses and are inserted here. */
    public long insert(Payslip p) {
        Long id = jdbc.queryForObject("SELECT seq_payroll.NEXTVAL FROM dual", Long.class);
        long payrollId = id == null ? 0 : id;
        jdbc.update("""
                INSERT INTO payroll (payroll_id, emp_id, pay_month, basic, hra, da, pf, gross, net)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, payrollId, p.empId(), p.month(), p.basic(), p.hra(), p.da(), p.pf(), p.gross(), p.net());
        return payrollId;
    }

    /** The PL/SQL path: procedure generate_payroll(emp, month, OUT payroll_id) does everything inside Oracle. */
    public long callGeneratePayroll(long empId, String month) {
        Long id = jdbc.execute((ConnectionCallback<Long>) con -> {
            try (CallableStatement cs = con.prepareCall("{call generate_payroll(?, ?, ?)}")) {
                cs.setLong(1, empId);
                cs.setString(2, month);
                cs.registerOutParameter(3, Types.NUMERIC);
                cs.execute();
                return cs.getLong(3);
            }
        });
        return id == null ? 0 : id;
    }

    public int callGeneratePayrollAll(String month) {
        Integer n = jdbc.execute((ConnectionCallback<Integer>) con -> {
            try (CallableStatement cs = con.prepareCall("{call generate_payroll_all(?, ?)}")) {
                cs.setString(1, month);
                cs.registerOutParameter(2, Types.NUMERIC);
                cs.execute();
                return cs.getInt(2);
            }
        });
        return n == null ? 0 : n;
    }

    public boolean exists(long empId, String month) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM payroll WHERE emp_id = ? AND pay_month = ?",
                Integer.class, empId, month);
        return n != null && n > 0;
    }

    public int deleteByMonth(String month) {
        return jdbc.update("DELETE FROM payroll WHERE pay_month = ?", month);
    }
}
