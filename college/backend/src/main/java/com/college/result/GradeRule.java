package com.college.result;

import java.math.BigDecimal;

/** One row of table GRADE: marks min..max give this grade, grade point and pass/fail. */
public record GradeRule(String code, BigDecimal minMark, BigDecimal maxMark, BigDecimal gradePoint, boolean pass) {

    public boolean covers(long roundedMark) {
        return BigDecimal.valueOf(roundedMark).compareTo(minMark) >= 0
                && BigDecimal.valueOf(roundedMark).compareTo(maxMark) <= 0;
    }
}
