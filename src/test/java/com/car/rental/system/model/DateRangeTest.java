package com.car.rental.system.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class DateRangeTest {

    static final LocalDate D1 = LocalDate.of(2026, 10, 1);

    @Test
    void countsDays() {
        assertEquals(3, DateRange.of(D1, 3).days());
    }

    @Test
    void rejectsEndBeforeOrEqualStart() {
        assertThrows(IllegalArgumentException.class, () -> new DateRange(D1, D1));
        assertThrows(IllegalArgumentException.class, () -> new DateRange(D1, D1.minusDays(1)));
    }

    // existing booking: Oct 5 .. Oct 8 (half-open)
    @ParameterizedTest(name = "[{0},{1}) overlaps [5,8)? {2}")
    @CsvSource({
            "1, 5, false",   // ends exactly when the other starts -> back-to-back is fine
            "8, 10, false",  // starts exactly when the other ends
            "4, 6, true",
            "6, 7, true",    // fully inside
            "3, 12, true",   // fully covers
            "7, 9, true"})
    void overlap(int startDay, int endDay, boolean expected) {
        DateRange existing = new DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8));
        DateRange requested = new DateRange(LocalDate.of(2026, 10, startDay), LocalDate.of(2026, 10, endDay));
        assertEquals(expected, existing.overlaps(requested));
        assertEquals(expected, requested.overlaps(existing), "overlap must be symmetric");
    }

    @Test
    void countsWeekendDays() {
        // Fri 2 Oct 2026 for 3 days -> Fri, Sat, Sun
        assertEquals(2, DateRange.of(LocalDate.of(2026, 10, 2), 3).weekendDays());
    }
}

