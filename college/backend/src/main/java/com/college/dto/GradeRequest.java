package com.college.dto;

import java.math.BigDecimal;

/** Change one row of the configurable grade scale. */
public record GradeRequest(BigDecimal minMark, BigDecimal maxMark, BigDecimal gradePoint, Boolean pass) {
}
