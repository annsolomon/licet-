package com.college.marks;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * THE EXACT MARK FORMULA OF THE SYLLABUS (do not change):
 *
 *   Part 1   = ASG1 (/40)  + (CT1 + CAT1) x 2/3     -> /100     (CT1 /30 + CAT1 /60 = /90 -> /60)
 *   Part 2   = ASG2 (/40)  + (CT2 + CAT2) x 2/3     -> /100
 *   Internal = (Part1 + Part2) / 5                  -> /40      (internal raw /200)
 *   Semester = SEM (/100) x 0.6                     -> /60
 *   Final    = Internal + Semester                  -> /100
 *
 * Worked example (Rahul, CS302): ASG1 35, CT1 27, CAT1 48, ASG2 32, CT2 24, CAT2 51, SEM 82
 *   (27+48) x 2/3 = 50     Part1 = 35+50 = 85
 *   (24+51) x 2/3 = 50     Part2 = 32+50 = 82
 *   Internal = 167/5 = 33.4     Semester = 82 x 0.6 = 49.2     FINAL = 82.6
 *
 * The same formula exists in Oracle (functions calculate_internal_mark / calculate_semester_mark).
 * Java is used for the live "show the working" screen and unit tests; Oracle stores the RESULT.
 * The API shows both values side by side so they can be proven equal.
 */
public final class MarkCalculator {

    private static final BigDecimal TWO_THIRDS_NUM = BigDecimal.valueOf(2);
    private static final BigDecimal THREE = BigDecimal.valueOf(3);
    private static final BigDecimal FIVE = BigDecimal.valueOf(5);
    private static final BigDecimal SEM_FACTOR = new BigDecimal("0.6");
    private static final MathContext PRECISE = new MathContext(20, RoundingMode.HALF_UP);

    private MarkCalculator() { }

    public static MarkBreakdown calculate(BigDecimal asg1, BigDecimal ct1, BigDecimal cat1,
                                          BigDecimal asg2, BigDecimal ct2, BigDecimal cat2,
                                          BigDecimal sem) {
        // convert (CT + CAT) from /90 to /60 : multiply by 2/3  (keep many digits, round only at the end)
        BigDecimal part1Converted = ct1.add(cat1).multiply(TWO_THIRDS_NUM).divide(THREE, PRECISE);
        BigDecimal part1 = asg1.add(part1Converted);

        BigDecimal part2Converted = ct2.add(cat2).multiply(TWO_THIRDS_NUM).divide(THREE, PRECISE);
        BigDecimal part2 = asg2.add(part2Converted);

        BigDecimal internalRaw = part1.add(part2);                         // out of 200
        BigDecimal internal = internalRaw.divide(FIVE, PRECISE);           // out of 40
        BigDecimal semester = sem.multiply(SEM_FACTOR);                    // out of 60

        BigDecimal internalR = round2(internal);
        BigDecimal semesterR = round2(semester);
        BigDecimal finalMark = round2(internalR.add(semesterR));

        List<String> steps = new ArrayList<>();
        steps.add("Part 1: (CT1 + CAT1) = " + plain(ct1) + " + " + plain(cat1) + " = " + plain(ct1.add(cat1))
                + "  ->  x 2/3 = " + plain(round2(part1Converted)));
        steps.add("Part 1 = ASG1 + converted = " + plain(asg1) + " + " + plain(round2(part1Converted))
                + " = " + plain(round2(part1)) + " / 100");
        steps.add("Part 2: (CT2 + CAT2) = " + plain(ct2) + " + " + plain(cat2) + " = " + plain(ct2.add(cat2))
                + "  ->  x 2/3 = " + plain(round2(part2Converted)));
        steps.add("Part 2 = ASG2 + converted = " + plain(asg2) + " + " + plain(round2(part2Converted))
                + " = " + plain(round2(part2)) + " / 100");
        steps.add("Internal raw = Part1 + Part2 = " + plain(round2(internalRaw)) + " / 200");
        steps.add("Internal = " + plain(round2(internalRaw)) + " / 5 = " + plain(internalR) + " / 40");
        steps.add("Semester = " + plain(sem) + " x 0.6 = " + plain(semesterR) + " / 60");
        steps.add("FINAL = " + plain(internalR) + " + " + plain(semesterR) + " = " + plain(finalMark) + " / 100");

        return new MarkBreakdown(asg1, ct1, cat1, asg2, ct2, cat2, sem,
                round2(part1Converted), round2(part1), round2(part2Converted), round2(part2),
                round2(internalRaw), internalR, semesterR, finalMark, steps);
    }

    /** Round half-up to 2 decimals (same as Oracle ROUND(x, 2) for positive numbers). */
    public static BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
