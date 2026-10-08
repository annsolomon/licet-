package com.college.dto;

/** The description of one DBMS syllabus topic shown in the DBMS Lab (no SQL results yet). */
public record DemoTopic(String id, int number, String category, String title, String file,
                        String projectUse, String explanation, String evaluatorSay, String viva,
                        String expected, java.util.List<String> statements) {
}
