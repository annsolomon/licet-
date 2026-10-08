package com.college.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * The configurable grade scale. The rules are LOADED FROM THE DATABASE (table GRADE) and passed in,
 * so changing a grade boundary in the Grades page needs no code change.
 *
 * Lookup rule (same as Oracle function calculate_grade): the final mark is rounded to a whole number
 * first (82.6 -> 83) and then matched against min..max (81..90 -> A+).
 */
public class GradeScale {

    private final List<GradeRule> rules;

    public GradeScale(List<GradeRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Optional<GradeRule> lookup(BigDecimal finalMark) {
        long rounded = finalMark.setScale(0, RoundingMode.HALF_UP).longValue();
        for (GradeRule rule : rules) {
            if (rule.covers(rounded)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    public List<GradeRule> rules() {
        return rules;
    }
}
