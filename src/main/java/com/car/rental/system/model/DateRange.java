package com.car.rental.system.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Half-open date range [start, end): a car picked up on the 1st and returned on the 4th is rented for 3 days,
 * and can be picked up again by someone else on the 4th.
 *
 * Immutable value object: final class, final fields, no setters, equality by value.
 */
public final class DateRange {

    private final LocalDate start;
    private final LocalDate end;

    public DateRange(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start: " + start + " -> " + end);
        }
        this.start = start;
        this.end = end;
    }

    public static DateRange of(LocalDate start, int days) {
        return new DateRange(start, start.plusDays(days));
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public long days() {
        return ChronoUnit.DAYS.between(start, end);
    }

    /** Two half-open ranges overlap iff each starts before the other ends. */
    public boolean overlaps(DateRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public long weekendDays() {
        long count = 0;
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            DayOfWeek day = d.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateRange)) return false;
        DateRange other = (DateRange) o;
        return start.equals(other.start) && end.equals(other.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "DateRange[" + start + ", " + end + ")";
    }
}
