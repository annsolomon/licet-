package com.college.repository;

import com.college.result.GradeRule;
import com.college.result.GradeScale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/** The configurable grade scale (table GRADE). Nothing about grades is hard-coded in Java. */
@Repository
public class GradeRepository {

    private static final RowMapper<GradeRule> MAPPER = (rs, i) -> new GradeRule(
            rs.getString("grade_code"), rs.getBigDecimal("min_mark"), rs.getBigDecimal("max_mark"),
            rs.getBigDecimal("grade_point"), "Y".equals(rs.getString("pass_flag")));

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<GradeRule> findAll() {
        return jdbc.query("SELECT grade_code, min_mark, max_mark, grade_point, pass_flag FROM grade ORDER BY grade_point DESC, min_mark DESC", MAPPER);
    }

    public GradeScale loadScale() {
        return new GradeScale(findAll());
    }

    public int update(String code, BigDecimal min, BigDecimal max, BigDecimal point, boolean pass) {
        return jdbc.update("UPDATE grade SET min_mark = ?, max_mark = ?, grade_point = ?, pass_flag = ? WHERE grade_code = ?",
                min, max, point, pass ? "Y" : "N", code);
    }
}
