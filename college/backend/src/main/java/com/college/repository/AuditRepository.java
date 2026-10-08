package com.college.repository;

import com.college.model.AuditEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Read-only access to AUDIT_LOG. Rows are written by Oracle TRIGGERS, never by Java. */
@Repository
public class AuditRepository {

    private static final RowMapper<AuditEntry> MAPPER = (rs, i) -> new AuditEntry(
            rs.getLong("audit_id"), rs.getString("table_name"), rs.getString("operation"),
            RsUtil.longValue(rs, "student_id"), rs.getString("record_key"),
            rs.getString("old_value"), rs.getString("new_value"), rs.getString("changed_by"),
            RsUtil.timestampText(rs, "changed_date"));

    private final JdbcTemplate jdbc;

    public AuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AuditEntry> findRecent(int limit) {
        return jdbc.query("""
                SELECT audit_id, table_name, operation, student_id, record_key, old_value, new_value, changed_by, changed_date
                  FROM audit_log ORDER BY audit_id DESC FETCH FIRST ? ROWS ONLY
                """, MAPPER, limit);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
        return n == null ? 0 : n;
    }
}
