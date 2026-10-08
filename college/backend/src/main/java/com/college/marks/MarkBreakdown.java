package com.college.marks;

import java.math.BigDecimal;
import java.util.List;

/**
 * Every intermediate number of the mark calculation, so the screen can SHOW the working.
 * Raw marks are inputs; nothing here is ever written back over the raw marks.
 *
 * @param part1Converted   (CT1 + CAT1) x 2/3          (out of 60)
 * @param part1            ASG1 + part1Converted        (out of 100)
 * @param part2Converted   (CT2 + CAT2) x 2/3
 * @param part2            ASG2 + part2Converted        (out of 100)
 * @param internalRaw      part1 + part2                (out of 200)
 * @param internal         internalRaw / 5              (out of 40)
 * @param semester         SEM x 0.6                    (out of 60)
 * @param finalMark        internal + semester          (out of 100)
 * @param steps            human readable working, one line per step
 */
public record MarkBreakdown(
        BigDecimal asg1, BigDecimal ct1, BigDecimal cat1,
        BigDecimal asg2, BigDecimal ct2, BigDecimal cat2,
        BigDecimal sem,
        BigDecimal part1Converted, BigDecimal part1,
        BigDecimal part2Converted, BigDecimal part2,
        BigDecimal internalRaw, BigDecimal internal,
        BigDecimal semester, BigDecimal finalMark,
        List<String> steps) {
}
