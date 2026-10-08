package com.college.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Small helpers for reading a JDBC ResultSet row.
 * A ResultSet is a cursor over the rows returned by a SELECT; rs.getString("COL"), rs.getLong("COL") ...
 * read one column of the CURRENT row. Oracle NULL needs special care for numbers: getInt returns 0 for
 * NULL, so we call wasNull() to find out.
 */
final class RsUtil {

    private RsUtil() { }

    static Integer integer(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    static Long longValue(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /** Oracle DATE -> yyyy-MM-dd text (the time part is ignored). */
    static String dateText(ResultSet rs, String column) throws SQLException {
        java.sql.Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate().toString();
    }

    static String timestampText(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime().withNano(0).toString().replace('T', ' ');
    }

    static java.sql.Date sqlDate(LocalDate date) {
        return java.sql.Date.valueOf(date);
    }
}
