package com.car.rental.system.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class DateRangeEdgeCaseTest {

    @Test
    void nullsAreRejected() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        assertThrows(NullPointerException.class, () -> new DateRange(null, d));
        assertThrows(NullPointerException.class, () -> new DateRange(d, null));
    }

    @Test
    void ofWithZeroOrNegativeDaysIsRejected() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        assertThrows(IllegalArgumentException.class, () -> DateRange.of(d, 0));
        assertThrows(IllegalArgumentException.class, () -> DateRange.of(d, -2));
    }

    @Test
    void daysAcrossMonthAndYearBoundaries() {
        assertEquals(3, new DateRange(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)).days());
        assertEquals(2, new DateRange(LocalDate.of(2028, 2, 28), LocalDate.of(2028, 3, 1)).days(), "2028 is a leap year");
        assertEquals(1, new DateRange(LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 1)).days());
    }

    @ParameterizedTest(name = "Oct {0} for {1} days -> {2} weekend days")
    @CsvSource({
            "5, 5, 0",    // Mon..Fri
            "5, 7, 2",    // one full week
            "10, 1, 1",   // Saturday only
            "11, 1, 1",   // Sunday only
            "9, 1, 0",    // Friday only
            "5, 14, 4",   // two weeks
            "10, 2, 2"})  // exactly a weekend
    void weekendDayCounting(int startDay, int days, long expectedWeekendDays) {
        assertEquals(expectedWeekendDays, DateRange.of(LocalDate.of(2026, 10, startDay), days).weekendDays());
    }

    @Test
    void endDateIsExclusiveForWeekendCounting() {
        // Fri 9 -> Sat 10: only Friday is rented, Saturday is the return day
        assertEquals(0, new DateRange(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10)).weekendDays());
    }

    @Test
    void rangeOverlapsItself() {
        DateRange r = DateRange.of(LocalDate.of(2026, 10, 1), 1);
        assertTrue(r.overlaps(r));
    }
}
