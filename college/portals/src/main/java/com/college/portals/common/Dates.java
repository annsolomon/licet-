package com.college.portals.common;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** Small helpers shared by the repositories. */
public final class Dates {
    private Dates() { }

    /** Validates yyyy-MM-dd (the value is later passed to TO_DATE as a bind variable). */
    public static String require(String text) {
        try {
            return LocalDate.parse(text).toString();
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.badRequest("Date must look like 2026-10-08");
        }
    }

    public static Map<String, Object> first(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }
}
