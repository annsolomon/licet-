package com.college.dto;

import java.math.BigDecimal;

public record SemesterGpa(int semester, BigDecimal sgpa, int credits) {
}
