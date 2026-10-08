package com.college.basics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Runs EVERY Java syllabus demo from the terminal (no database, no Spring needed):
 *
 *     ./run.sh syllabus
 *
 * This is the quickest way to show the Java syllabus to an evaluator.
 */
public class SyllabusRunner {

    public static void main(String[] args) throws Exception {
        Path temp = args.length > 0 ? Path.of(args[0]) : Files.createTempDirectory("syllabus-demo");
        Map<String, List<String>> demos = SyllabusDemos.all(temp);
        int number = 0;
        for (Map.Entry<String, List<String>> demo : demos.entrySet()) {
            number++;
            System.out.println();
            System.out.println("=== [" + number + "/" + demos.size() + "] " + demo.getKey() + " ===");
            demo.getValue().forEach(line -> System.out.println("  " + line));
        }
        System.out.println();
        System.out.println("All " + demos.size() + " syllabus demos ran successfully.");
    }
}
