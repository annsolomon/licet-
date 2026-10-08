package com.college.exception;

/**
 * SYLLABUS: EXCEPTION HANDLING - user-defined exceptions.
 *
 *   Throwable
 *     '-- Exception
 *          '-- RuntimeException          (unchecked: no "throws" needed)
 *               '-- CollegeException     (base of all our own exceptions)
 *                    |-- InvalidStudentException
 *                    |-- InvalidMarksException
 *                    |-- StudentNotFoundException
 *                    |-- ResourceNotFoundException
 *                    |-- DuplicateResourceException
 *                    '-- DatabaseException
 *
 * GlobalExceptionHandler catches them and converts each into a clean HTTP response.
 */
public class CollegeException extends RuntimeException {

    public CollegeException(String message) {
        super(message);
    }

    public CollegeException(String message, Throwable cause) {
        super(message, cause);
    }
}
