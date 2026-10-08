package com.college;

import com.college.exception.InvalidMarksException;
import com.college.marks.MarkBreakdown;
import com.college.marks.MarkCalculator;
import com.college.marks.MarkValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The exact example of the specification: Rahul / CS302 = 33.4 + 49.2 = 82.6 */
class MarkCalculatorTest {

    private static BigDecimal b(String s) { return new BigDecimal(s); }

    private MarkBreakdown rahul() {
        return MarkCalculator.calculate(b("35"), b("27"), b("48"), b("32"), b("24"), b("51"), b("82"));
    }

    @Test
    void internalIs33Point4() {
        assertEquals(0, b("33.40").compareTo(MarkCalculator.round2(rahul().internal())));
    }

    @Test
    void semesterIs49Point2() {
        assertEquals(0, b("49.20").compareTo(MarkCalculator.round2(rahul().semester())));
    }

    @Test
    void finalIs82Point6() {
        assertEquals(0, b("82.60").compareTo(MarkCalculator.round2(rahul().finalMark())));
    }

    @Test
    void part1AndPart2() {
        MarkBreakdown m = rahul();
        assertEquals(0, b("85").compareTo(MarkCalculator.round2(m.part1())));   // 35 + (27+48)*2/3 = 85
        assertEquals(0, b("82").compareTo(MarkCalculator.round2(m.part2())));   // 32 + (24+51)*2/3 = 82
    }

    @Test
    void validatorRejectsAboveMaximum() {
        assertThrows(InvalidMarksException.class, () -> MarkValidator.validate("CT1", b("31"), b("30")));
    }

    @Test
    void validatorRejectsNegative() {
        assertThrows(InvalidMarksException.class, () -> MarkValidator.validate("CT1", b("-1"), b("30")));
    }

    @Test
    void validatorAcceptsMaximum() {
        MarkValidator.validate("CT1", b("30"), b("30"));
    }
}
