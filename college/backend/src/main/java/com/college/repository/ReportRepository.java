package com.college.repository;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;

/** Calls the PL/SQL procedures that return TEXT through an OUT parameter (cursor and exception demos). */
@Repository
public class ReportRepository {

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Calls department_report_text(dept_id, OUT report). Inside Oracle this procedure uses an EXPLICIT CURSOR
     * (DECLARE CURSOR / OPEN / FETCH / EXIT / CLOSE).
     *
     *   React -> Spring Boot -> JdbcTemplate -> {call department_report_text(?, ?)} -> Oracle -> OUT text -> JSON
     */
    public String departmentReport(long deptId) {
        return jdbc.execute((ConnectionCallback<String>) con -> {
            try (CallableStatement cs = con.prepareCall("{call department_report_text(?, ?)}")) {
                cs.setLong(1, deptId);
                cs.registerOutParameter(2, Types.VARCHAR);
                cs.execute();
                return cs.getString(2);
            }
        });
    }
}
