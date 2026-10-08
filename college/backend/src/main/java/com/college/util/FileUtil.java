package com.college.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SYLLABUS: FILE OPERATIONS.
 *   1. write a text file (report export)
 *   2. COUNT CHARACTER OCCURRENCES in a file
 *   3. COPY one file to another
 *
 * Streams used: FileReader/BufferedReader (read characters), FileWriter/BufferedWriter (write characters).
 * try-with-resources closes the streams automatically, even when an exception happens.
 */
public final class FileUtil {

    private FileUtil() { }

    /** Write all lines to the file (creates folders when needed). @return number of characters written */
    public static long writeLines(Path file, List<String> lines) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        long chars = 0;
        try (BufferedWriter out = new BufferedWriter(new FileWriter(file.toFile()))) {
            for (String line : lines) {
                out.write(line);
                out.newLine();
                chars += line.length() + 1;
            }
        }
        return chars;
    }

    /** COUNT how many times the character occurs in the file (case-sensitive). */
    public static int countOccurrences(Path file, char target) throws IOException {
        int count = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(file.toFile()))) {
            int c;
            while ((c = in.read()) != -1) {        // read() returns -1 at end of file
                if ((char) c == target) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Count every character in the file. */
    public static long countCharacters(Path file) throws IOException {
        long count = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(file.toFile()))) {
            while (in.read() != -1) {
                count++;
            }
        }
        return count;
    }

    /** COPY source -> target character by character. @return characters copied */
    public static long copyFile(Path source, Path target) throws IOException {
        long copied = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(source.toFile()));
             BufferedWriter out = new BufferedWriter(new FileWriter(target.toFile()))) {
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
                copied++;
            }
        }
        return copied;
    }

    /** Read all lines (used for the report preview). */
    public static List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file);
    }
}
