package com.car.rental.system.repository;

import com.car.rental.system.model.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryRepositoryTest {

    @Nested
    class Vehicles {
        final VehicleRepository repo = new InMemoryVehicleRepository();

        @Test
        void findByTypeIsFilteredAndSortedByPlate() {
            repo.save(new Vehicle("S2", "Ford", "Edge", VehicleType.SUV, 0));
            repo.save(new Vehicle("E1", "Toyota", "Yaris", VehicleType.SEDAN, 0));
            repo.save(new Vehicle("S1", "Kia", "Sorento", VehicleType.SUV, 0));

            assertEquals(Arrays.asList("S1", "S2"), repo.findByType(VehicleType.SUV).stream().map(Vehicle::plate).collect(Collectors.toList()));
            assertEquals(Arrays.asList("E1", "S1", "S2"), repo.findAll().stream().map(Vehicle::plate).collect(Collectors.toList()));
            assertTrue(repo.findByType(VehicleType.VAN).isEmpty());
        }

        @Test
        void saveIsAnUpsertKeyedByPlate() {
            repo.save(new Vehicle("P1", "Toyota", "Yaris", VehicleType.SEDAN, 0));
            repo.save(new Vehicle("P1", "Toyota", "Yaris", VehicleType.SEDAN, 500));
            assertEquals(1, repo.findAll().size());
            assertEquals(500, repo.findByPlate("P1").get().mileage());
        }

        @Test
        void unknownPlateIsEmptyNotNull() {
            assertFalse(repo.findByPlate("missing").isPresent());
        }

        @Test
        void returnedListsAreImmutableSnapshots() {
            repo.save(new Vehicle("P1", "Toyota", "Yaris", VehicleType.SEDAN, 0));
            List<Vehicle> all = repo.findAll();
            assertThrows(UnsupportedOperationException.class, () -> all.clear());
        }
    }

    @Nested
    class Reservations {
        final ReservationRepository repo = new InMemoryReservationRepository();
        final Customer alice = new Customer("C1", "Alice", "DL1", false);
        final Customer bob = new Customer("C2", "Bob", "DL2", false);
        final Vehicle e1 = new Vehicle("E1", "Toyota", "Yaris", VehicleType.SEDAN, 0);
        final Vehicle e2 = new Vehicle("E2", "Toyota", "Yaris", VehicleType.SEDAN, 0);
        final DateRange range = DateRange.of(LocalDate.of(2026, 10, 1), 2);

        @Test
        void findsByVehicleAndByCustomer() {
            repo.save(new Reservation("R1", alice, e1, range, BigDecimal.ONE));
            repo.save(new Reservation("R2", bob, e1, range, BigDecimal.ONE));
            repo.save(new Reservation("R3", alice, e2, range, BigDecimal.ONE));

            assertEquals(2, repo.findByVehicle("E1").size());
            assertEquals(2, repo.findByCustomer("C1").size());
            assertEquals(3, repo.findAll().size());
            assertTrue(repo.findByCustomer("nobody").isEmpty());
        }

        @Test
        void findByIdRoundTrips() {
            Reservation r = new Reservation("R1", alice, e1, range, BigDecimal.ONE);
            repo.save(r);
            assertSame(r, repo.findById("R1").get());
            assertFalse(repo.findById("R404").isPresent());
        }
    }
}

