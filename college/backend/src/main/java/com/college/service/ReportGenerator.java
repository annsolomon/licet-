package com.college.service;

import com.college.dto.ReportRequest;

import java.util.List;

/**
 * SYLLABUS: INTERFACE - one report = one ReportGenerator.
 * ReportService keeps a Map of them (type name -> generator) and calls generate() through the interface,
 * so adding a new report only means adding one more implementation.
 */
public interface ReportGenerator {

    /** e.g. STUDENT, ATTENDANCE, MARKS ... */
    String type();

    /** The lines of the report (the title is added by ReportService). */
    List<String> generate(ReportRequest request);
}
