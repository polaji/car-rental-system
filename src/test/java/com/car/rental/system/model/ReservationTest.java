package com.car.rental.system.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.car.rental.system.model.Reservation.Status;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

    final Customer customer1 = new Customer("C1", "Alice", "DL1", false);
    final Vehicle car = new Vehicle("E1", "Toyota", "Yaris", VehicleType.SEDAN, 0);
    final DateRange oct5to8 = new DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8));
    Reservation r;

    @BeforeEach
    void setUp() {
        r = new Reservation("R1", customer1, car, oct5to8, new BigDecimal("135.00"));
    }

    @Test
    void startsInReservedState() {
        assertEquals(Status.RESERVED, r.status());
    }

    /** Full truth table of the lifecycle: every (from, to) pair, allowed or not. */
    @ParameterizedTest(name = "{0} -> {1} allowed? {2}")
    @CsvSource({
            "RESERVED,  RESERVED,  false",
            "RESERVED,  ACTIVE,    true",
            "RESERVED,  COMPLETED, false",   // can't return a car that was never picked up
            "RESERVED,  CANCELLED, true",
            "ACTIVE,    RESERVED,  false",
            "ACTIVE,    ACTIVE,    false",
            "ACTIVE,    COMPLETED, true",
            "ACTIVE,    CANCELLED, false",   // can't cancel once the customer has the keys
            "COMPLETED, ACTIVE,    false",   // terminal
            "COMPLETED, CANCELLED, false",
            "CANCELLED, ACTIVE,    false",   // terminal
            "CANCELLED, RESERVED,  false"})
    void lifecycleTransitions(Status from, Status to, boolean allowed) {
        moveTo(from);
        if (allowed) {
            r.transitionTo(to);
            assertEquals(to, r.status());
        } else {
            assertThrows(IllegalStateException.class, () -> r.transitionTo(to));
            assertEquals(from, r.status(), "rejected transition must not change state");
        }
    }

    @Test
    void checkTransitionValidatesWithoutMutating() {
        r.checkTransition(Status.ACTIVE);
        assertEquals(Status.RESERVED, r.status());
        assertThrows(IllegalStateException.class, () -> r.checkTransition(Status.COMPLETED));
    }

    @Test
    void reservedAndActiveBookingsBlockOverlappingDates() {
        DateRange overlapping = new DateRange(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 9));
        assertTrue(r.blocks(overlapping));
        r.transitionTo(Status.ACTIVE);
        assertTrue(r.blocks(overlapping));
    }

    @Test
    void cancelledOrCompletedBookingsNoLongerBlock() {
        r.transitionTo(Status.CANCELLED);
        assertFalse(r.blocks(oct5to8));

        Reservation done = new Reservation("R2", customer1, car, oct5to8, BigDecimal.TEN);
        done.transitionTo(Status.ACTIVE);
        done.transitionTo(Status.COMPLETED);
        assertFalse(done.blocks(oct5to8));
    }

    @Test
    void nonOverlappingDatesAreNeverBlocked() {
        assertFalse(r.blocks(DateRange.of(LocalDate.of(2026, 10, 8), 2)));
    }

    @Test
    void identityIsTheReservationId() {
        Reservation sameId = new Reservation("R1", customer1, car, DateRange.of(LocalDate.of(2027, 1, 1), 1), BigDecimal.ONE);
        assertEquals(r, sameId);
        assertEquals(r.hashCode(), sameId.hashCode());
    }

    @Test
    void allFieldsAreMandatory() {
        assertThrows(NullPointerException.class, () -> new Reservation(null, customer1, car, oct5to8, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> new Reservation("R", customer1, car, oct5to8, null));
    }

    private void moveTo(Status target) {
        switch (target) {
            case RESERVED:
                break;
            case ACTIVE:
                r.transitionTo(Status.ACTIVE);
                break;
            case COMPLETED:
                r.transitionTo(Status.ACTIVE);
                r.transitionTo(Status.COMPLETED);
                break;
            case CANCELLED:
                r.transitionTo(Status.CANCELLED);
                break;
        }
    }
}

