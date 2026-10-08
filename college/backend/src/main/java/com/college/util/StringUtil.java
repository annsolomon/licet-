package com.college.util;

import java.util.regex.Pattern;

/**
 * SYLLABUS: STRING HANDLING.
 *
 * Used by the project for: student names, department names, subject codes,
 * validation and search. Every method is small so it can be explained in the viva.
 */
public final class StringUtil {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern SUBJECT_CODE = Pattern.compile("^[A-Z]{2,3}[0-9]{3}$");

    private StringUtil() { }   // utility class: no objects

    /** "  rahul   KUMAR " -> "Rahul Kumar"  (trim, collapse spaces, capitalise each word) */
    public static String toTitleCase(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /** " cs302 " -> "CS302" */
    public static String normalizeSubjectCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    /** CS302 = 2-3 letters + 3 digits */
    public static boolean isValidSubjectCode(String code) {
        return code != null && SUBJECT_CODE.matcher(code).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    /** A name may contain letters, spaces, dots and apostrophes only, 2..100 characters. */
    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2 && name.trim().length() <= 100
                && name.matches("[A-Za-z .']+");
    }

    /** Case-insensitive "contains" used by the search box. */
    public static boolean containsIgnoreCase(String text, String keyword) {
        return text != null && keyword != null && text.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    public static String reverse(String text) {
        return new StringBuilder(text).reverse().toString();
    }

    public static boolean isPalindrome(String text) {
        String clean = text.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return clean.equals(reverse(clean));
    }

    public static int countVowels(String text) {
        int count = 0;
        for (char c : text.toLowerCase().toCharArray()) {
            if ("aeiou".indexOf(c) >= 0) {
                count++;
            }
        }
        return count;
    }

    /** Initials of a name: "Rahul Kumar Sharma" -> "RKS" */
    public static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }
}
