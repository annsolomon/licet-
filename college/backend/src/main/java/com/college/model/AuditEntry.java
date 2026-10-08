package com.college.model;

/** One row of AUDIT_LOG (written by Oracle triggers, read-only for the application). */
public record AuditEntry(long id, String tableName, String operation, Long studentId, String recordKey,
                         String oldValue, String newValue, String changedBy, String changedDate) {
}
