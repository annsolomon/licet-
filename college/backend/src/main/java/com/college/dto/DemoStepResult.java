package com.college.dto;

import java.util.List;

/**
 * Result of ONE statement in a DBMS Lab demo:
 * kind = QUERY | DML | DDL | CONTROL | PLSQL | EXPECT_ERROR
 */
public record DemoStepResult(String sql, String kind, String note, List<String> columns,
                             List<List<String>> rows, String message, boolean error) {
}
