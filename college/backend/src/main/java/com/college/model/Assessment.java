package com.college.model;

import java.math.BigDecimal;

/** One row of ASSESSMENT (ASG1, CT1, CAT1, ASG2, CT2, CAT2, SEM) with its maximum raw mark. */
public record Assessment(long id, String code, String name, BigDecimal maxMarks, int part) {
}
