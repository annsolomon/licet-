package com.college.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * SGPA / CGPA = SUM(grade point x credits) / SUM(credits).
 *
 *   SGPA : over the subjects of ONE semester
 *   CGPA : over the subjects of ALL semesters
 *
 * Example: A+ (9) in a 4-credit subject and O (10) in a 3-credit subject
 *   (9x4 + 10x3) / (4+3) = 66 / 7 = 9.43
 *
 * Same formula as Oracle functions calculate_sgpa / calculate_cgpa.
 */
public final class GradePointCalculator {

    /** One subject's contribution. */
    public record SubjectGrade(BigDecimal gradePoint, int credits) { }

    private GradePointCalculator() { }

    /** @return the GPA rounded to 2 decimals, or null when there are no credits */
    public static BigDecimal gpa(List<SubjectGrade> subjects) {
        BigDecimal points = BigDecimal.ZERO;
        int credits = 0;
        for (SubjectGrade s : subjects) {
            points = points.add(s.gradePoint().multiply(BigDecimal.valueOf(s.credits())));
            credits += s.credits();
        }
        if (credits == 0) {
            return null;
        }
        return points.divide(BigDecimal.valueOf(credits), 2, RoundingMode.HALF_UP);
    }
}
