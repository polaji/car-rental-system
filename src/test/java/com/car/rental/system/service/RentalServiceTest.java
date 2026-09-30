package com.car.rental.system.service;

import com.car.rental.system.model.*;
import com.car.rental.system.repository.InMemoryReservationRepository;
import com.car.rental.system.repository.InMemoryVehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RentalServiceTest {

    InMemoryVehicleRepository fleet;
    RentalService service;
    final Customer customer1 = new Customer("C1", "Customer1", "DL1", false);
    final Customer customer2 = new Customer("C2", "Customer2", "DL2", false);
    final DateRange oct5to8 = new DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8));

    @BeforeEach
    void setUp() {
        fleet = new InMemoryVehicleRepository();
        fleet.save(new Vehicle("E1", "Toyota", "Yaris", VehicleType.SEDAN, 0));
        fleet.save(new Vehicle("S1", "Ford", "Edge", VehicleType.SUV, 0));

        service = new RentalService(fleet, new InMemoryReservationRepository(),
                (type, period, customer) -> BigDecimal.valueOf(100 * period.days()));
    }

    @Test
    void reservesAnAvailableCar() {
        Reservation r = service.reserve(customer1, VehicleType.SEDAN, oct5to8);
        assertEquals("E1", r.vehicle().plate());
        assertEquals(Reservation.Status.RESERVED, r.status());
        assertEquals(new BigDecimal("300"), r.price());
        assertTrue(service.findAvailable(VehicleType.SEDAN, oct5to8).isEmpty());
    }

    @Test
    void rejectsBookingWhenNoCarIsFree() {
        service.reserve(customer1, VehicleType.SEDAN, oct5to8);
        DateRange overlapping = new DateRange(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 9));
        RentalException ex = assertThrows(RentalException.NoAvailabilityException.class,
                () -> service.reserve(customer2, VehicleType.SEDAN, overlapping));
        assertTrue(ex.getMessage().contains("SEDAN"));
    }

    @Test
    void allowsBackToBackBookings() {
        service.reserve(customer1, VehicleType.SEDAN, oct5to8);
        Reservation next = service.reserve(customer2, VehicleType.SEDAN, DateRange.of(LocalDate.of(2026, 10, 8), 2));
        assertEquals("E1", next.vehicle().plate());
    }

    @Test
    void cancellingFreesTheCar() {
        Reservation r = service.reserve(customer1, VehicleType.SEDAN, oct5to8);
        service.cancel(r.id());
        assertEquals(1, service.findAvailable(VehicleType.SEDAN, oct5to8).size());
    }

    @Test
    void carsInMaintenanceAreNotOffered() {
        fleet.findByPlate("E1").get().sendToMaintenance();
        assertThrows(RentalException.NoAvailabilityException.class, () -> service.reserve(customer1, VehicleType.SEDAN, oct5to8));
    }

    @Nested
    class Lifecycle {
        @Test
        void pickupAndReturnCompletesReservationAndUpdatesMileage() {
            Reservation r = service.reserve(customer1, VehicleType.SUV, oct5to8);
            service.pickUp(r.id());
            assertEquals(Vehicle.Status.RENTED, r.vehicle().status());

            service.returnVehicle(r.id(), 350);

            assertEquals(Reservation.Status.COMPLETED, r.status());
            assertEquals(Vehicle.Status.AVAILABLE, r.vehicle().status());
            assertEquals(350, r.vehicle().mileage());
            assertEquals(new BigDecimal("300"), service.revenueByType().get(VehicleType.SUV));
        }

        @Test
        void cannotCancelAnActiveRental() {
            Reservation r = service.reserve(customer1, VehicleType.SUV, oct5to8);
            service.pickUp(r.id());
            assertThrows(IllegalStateException.class, () -> service.cancel(r.id()));
        }

        @Test
        void unknownReservationIsNotFound() {
            assertThrows(RentalException.NotFoundException.class, () -> service.pickUp("nope"));
        }
    }

    @Test
    void concurrentBookingsNeverDoubleBookTheSameCar() throws Exception {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Customer customer = new Customer("C" + i, "User" + i, "DL" + i, false);
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    service.reserve(customer, VehicleType.SEDAN, oct5to8);
                    successes.incrementAndGet();
                } catch (RentalException.NoAvailabilityException e) {
                    rejections.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, successes.get(), "only one ECONOMY car exists");
        assertEquals(threads - 1, rejections.get());
    }
}

