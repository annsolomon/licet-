package com.college.util;

import java.util.List;

/**
 * SYLLABUS: GENERIC AVERAGE.
 *
 * LEVEL 1 : GenericAverage&lt;Integer&gt; of {10,20,30} = 20.0
 * LEVEL 2 : GenericAverage&lt;BigDecimal&gt; of a list of final marks (department average)
 *
 * "T extends Number" is a BOUND: T may only be a numeric type (Integer, Double, BigDecimal ...).
 * Every Number can give itself as a double with doubleValue(), so one class serves all.
 */
public class GenericAverage<T extends Number> {

    private final List<T> numbers;

    public GenericAverage(List<T> numbers) {
        this.numbers = numbers;
    }

    /** @return arithmetic mean, or 0.0 for an empty list */
    public double average() {
        if (numbers.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (T n : numbers) {
            sum += n.doubleValue();
        }
        return sum / numbers.size();
    }
}
