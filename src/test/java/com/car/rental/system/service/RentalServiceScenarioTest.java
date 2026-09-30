package com.car.rental.system.service;

import com.car.rental.system.model.*;
import com.car.rental.system.pricing.StandardPricing;
import com.car.rental.system.repository.InMemoryReservationRepository;
import com.car.rental.system.repository.InMemoryVehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalServiceScenarioTest {

    InMemoryVehicleRepository fleet;
    RentalService service;
    final Customer customer1 = new Customer("C1", "Customer1", "DL1", false);
    final Customer customer2 = new Customer("C2", "Customer2", "DL2", true);
    final LocalDate mon = LocalDate.of(2026, 10, 5);
    final DateRange monToThu = DateRange.of(mon, 3);

    @BeforeEach
    void setUp() {
        fleet = new InMemoryVehicleRepository();
        fleet.save(new Vehicle("E1", "Toyota", "Yaris", VehicleType.SEDAN, 100));
        fleet.save(new Vehicle("E2", "Honda", "Civic", VehicleType.SEDAN, 200));
        fleet.save(new Vehicle("V1", "Ford", "Transit", VehicleType.VAN, 300));
        service = new RentalService(fleet, new InMemoryReservationRepository(), new StandardPricing());
    }

    @Nested
    class Booking {
        @Test
        void secondCustomerGetsTheNextFreeCarOfTheSameType() {
            Reservation first = service.reserve(customer1, VehicleType.SEDAN, monToThu);
            Reservation second = service.reserve(customer2, VehicleType.SEDAN, monToThu);

            assertEquals("E1", first.vehicle().plate());
            assertEquals("E2", second.vehicle().plate());
            assertThrows(RentalException.NoAvailabilityException.class,
                    () -> service.reserve(customer1, VehicleType.SEDAN, monToThu));
        }

        @Test
        void bookingOneTypeDoesNotAffectAnother() {
            service.reserve(customer1, VehicleType.VAN, monToThu);
            assertEquals(2, service.findAvailable(VehicleType.SEDAN, monToThu).size());
            assertTrue(service.findAvailable(VehicleType.VAN, monToThu).isEmpty());
        }

        @Test
        void typeWithNoCarsInFleetIsUnavailable() {
            assertThrows(RentalException.NoAvailabilityException.class,
                    () -> service.reserve(customer1, VehicleType.SUV, monToThu));
        }

        @Test
        void priceIsLockedAtBookingTimeFromTheStrategy() {
            Reservation r = service.reserve(customer2, VehicleType.SEDAN, monToThu);   // loyal: 135 * 0.95
            assertEquals(new BigDecimal("128.25"), r.price());
            assertEquals(r.price(), service.quote(VehicleType.SEDAN, monToThu, customer2), "quote == booked price");
        }

        @Test
        void reservationIdsAreUnique() {
            Reservation a = service.reserve(customer1, VehicleType.SEDAN, monToThu);
            Reservation b = service.reserve(customer2, VehicleType.SEDAN, monToThu);
            assertNotEquals(a.id(), b.id());
        }

        @Test
        void completedRentalFreesTheCarForTheSameDatesInTheFuture() {
            Reservation r = service.reserve(customer1, VehicleType.VAN, monToThu);
            service.pickUp(r.id());
            service.returnVehicle(r.id(), 400);
            assertEquals(1, service.findAvailable(VehicleType.VAN, monToThu).size());
        }

        @Test
        void carCanBeBookedForManyNonOverlappingPeriods() {
            for (int week = 0; week < 10; week++) {
                Reservation r = service.reserve(customer1, VehicleType.VAN, DateRange.of(mon.plusWeeks(week), 7));
                assertEquals("V1", r.vehicle().plate());
            }
            assertEquals(10, service.history("C1").size());
        }
    }

    @Nested
    class LifecycleGuards {
        @Test
        void pickingUpACancelledReservationFails_andLeavesTheCarAvailable() {
            Reservation r = service.reserve(customer1, VehicleType.VAN, monToThu);
            service.cancel(r.id());

            assertThrows(IllegalStateException.class, () -> service.pickUp(r.id()));
            assertEquals(Vehicle.Status.AVAILABLE, r.vehicle().status(),
                    "regression: vehicle used to be flipped to RENTED before the reservation check failed");
        }

        @Test
        void returningABookingThatWasNeverPickedUpFails() {
            Reservation r = service.reserve(customer1, VehicleType.VAN, monToThu);
            assertThrows(IllegalStateException.class, () -> service.returnVehicle(r.id(), 999));
            assertEquals(Reservation.Status.RESERVED, r.status());
        }

        @Test
        void cannotPickUpWhileAPreviousCustomerStillHasTheCar() {
            Reservation reservation1 = service.reserve(customer1, VehicleType.VAN, monToThu);
            Reservation reservation2 = service.reserve(customer2, VehicleType.VAN, DateRange.of(monToThu.end(), 2));
            service.pickUp(reservation1.id());

            assertThrows(IllegalStateException.class, () -> service.pickUp(reservation2.id()));
            assertEquals(Reservation.Status.RESERVED, reservation2.status(), "Customer2's booking is untouched and can retry");
        }

        @Test
        void doubleCancelIsRejected() {
            Reservation r = service.reserve(customer1, VehicleType.VAN, monToThu);
            service.cancel(r.id());
            assertThrows(IllegalStateException.class, () -> service.cancel(r.id()));
        }

        @Test
        void badOdometerOnReturnKeepsTheRentalActive() {
            Reservation r = service.reserve(customer1, VehicleType.VAN, monToThu);
            service.pickUp(r.id());
            assertThrows(IllegalArgumentException.class, () -> service.returnVehicle(r.id(), 10));
            assertEquals(Reservation.Status.ACTIVE, r.status());
            assertEquals(Vehicle.Status.RENTED, r.vehicle().status());
        }

        @Test
        void unknownIdsAreNotFoundForEveryOperation() {
            assertThrows(RentalException.NotFoundException.class, () -> service.cancel("X"));
            assertThrows(RentalException.NotFoundException.class, () -> service.returnVehicle("X", 1));
        }
    }

}

