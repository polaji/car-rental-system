package com.car.rental.system.service;

import com.car.rental.system.model.*;
import com.car.rental.system.repository.InMemoryReservationRepository;
import com.car.rental.system.repository.InMemoryVehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalServiceConcurrencyTest {

    static final int THREADS = 32;
    static final int ROUNDS = 20;
    final DateRange oct5to8 = new DateRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8));

    InMemoryVehicleRepository fleet;
    InMemoryReservationRepository bookings;
    RentalService service;

    @BeforeEach
    void setUp() {
        fleet = new InMemoryVehicleRepository();
        bookings = new InMemoryReservationRepository();
        service = new RentalService(fleet, bookings, (t, p, c) -> BigDecimal.valueOf(p.days()));
    }

    @Test
    void nCarsMeansExactlyNWinners_andNoCarIsDoubleBooked() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            setUp();
            for (int i = 1; i <= 3; i++) fleet.save(new Vehicle("S" + i, "Kia", "Sorento", VehicleType.SUV, 0));
            List<Object> outcomes = race(THREADS, i -> service.reserve(customer(i), VehicleType.SUV, oct5to8));
            List<Reservation> wins = outcomes.stream().filter(Reservation.class::isInstance).map(Reservation.class::cast).collect(Collectors.toList());
            assertEquals(3, wins.size(), "round " + round);
            assertEquals(3, new HashSet<>(wins.stream().map(r -> r.vehicle().plate()).collect(Collectors.toList())).size(), "each car once");
            assertEquals(THREADS - 3, outcomes.stream().filter(RentalException.NoAvailabilityException.class::isInstance).count());
        }
    }

    @Test
    void nonOverlappingRequestsForTheSameCarAllSucceed() throws Exception {
        fleet.save(new Vehicle("V1", "Ford", "Transit", VehicleType.VAN, 0));
        LocalDate base = LocalDate.of(2026, 11, 1);

        // each thread wants a different single day -> lock serialises them but nobody should be rejected
        List<Object> outcomes = race(THREADS, i -> service.reserve(customer(i), VehicleType.VAN, DateRange.of(base.plusDays(i), 1)));

        assertTrue(outcomes.stream().allMatch(Reservation.class::isInstance), outcomes.toString());
        assertEquals(THREADS, bookings.findByVehicle("V1").size());
    }

    @Test
    void reservationIdsStayUniqueUnderContention() throws Exception {
        for (int i = 0; i < THREADS; i++) fleet.save(new Vehicle(String.format("E%02d", i), "Toyota", "Yaris", VehicleType.SEDAN, 0));

        List<Object> outcomes = race(THREADS, i -> service.reserve(customer(i), VehicleType.SEDAN, oct5to8));

        Set<String> ids = new HashSet<>();
        for (Object o : outcomes) assertTrue(ids.add(((Reservation) o).id()), "duplicate id " + o);
        assertEquals(THREADS, ids.size());
    }

    @Test
    void cancelRacingWithReserveNeverProducesTwoLiveBookings() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            setUp();
            fleet.save(new Vehicle("E1", "Toyota", "Yaris", VehicleType.SEDAN, 0));
            Reservation existing = service.reserve(customer(999), VehicleType.SEDAN, oct5to8);

            // half the threads try to book, one thread cancels the existing booking
            race(THREADS, i -> i == 0 ? service.cancel(existing.id()) : service.reserve(customer(i), VehicleType.SEDAN, oct5to8));

            long live = bookings.findByVehicle("E1").stream().filter(r -> r.blocks(oct5to8)).count();
            assertTrue(live <= 1, "round " + round + ": " + live + " live bookings for one car");
        }
    }

    // ---- helpers ------------------------------------------------------------------------------

    interface Task { Object run(int i) throws Exception; }

    /** Runs n tasks that all start at the same instant; returns each task's result or thrown exception. */
    private static List<Object> race(int n, Task task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int id = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    return task.run(id);
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<Object> f : futures) results.add(f.get(10, TimeUnit.SECONDS));   // also detects deadlock
        pool.shutdownNow();
        return results;
    }

    private static Customer customer(int i) {
        return new Customer("C" + i, "User" + i, "DL" + i, false);
    }
}

