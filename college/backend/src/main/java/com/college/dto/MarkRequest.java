package com.college.dto;

import java.math.BigDecimal;

/** Enter or correct ONE raw mark: {"studentId":101,"subjectId":2,"assessmentCode":"CT1","rawMarks":27} */
public record MarkRequest(Long studentId, Long subjectId, String assessmentCode, BigDecimal rawMarks) {
}
