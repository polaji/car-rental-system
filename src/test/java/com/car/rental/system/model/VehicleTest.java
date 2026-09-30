package com.car.rental.system.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Vehicle is a small state machine: AVAILABLE -> RENTED -> AVAILABLE, and anything -> MAINTENANCE. */
public class VehicleTest {

    Vehicle car;

    @BeforeEach
    void setUp() {
        car = new Vehicle("ABC-123", "Toyota", "Camry", VehicleType.SEDAN, 1_000);
    }

    @Test
    void newVehicleIsAvailable() {
        assertEquals(Vehicle.Status.AVAILABLE, car.status());
        assertEquals(1_000, car.mileage());
    }

    @Test
    void constructorRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle(" ", "Toyota", "Camry", VehicleType.SEDAN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Vehicle(null, "Toyota", "Camry", VehicleType.SEDAN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("P1", "Toyota", "Camry", VehicleType.SEDAN, -1));
        assertThrows(NullPointerException.class, () -> new Vehicle("P1", "Toyota", "Camry", null, 0));
    }

    @Test
    void pickUpThenDropOffUpdatesMileage() {
        car.pickUp();
        assertEquals(Vehicle.Status.RENTED, car.status());

        car.dropOff(1_250);

        assertEquals(Vehicle.Status.AVAILABLE, car.status());
        assertEquals(1_250, car.mileage());
    }

    @Test
    void cannotPickUpTwice() {
        car.pickUp();
        assertThrows(IllegalStateException.class, car::pickUp);
    }

    @Test
    void cannotDropOffACarThatIsNotRented() {
        assertThrows(IllegalStateException.class, () -> car.dropOff(2_000));
    }

    @Test
    void odometerCannotGoBackwards_andStateIsUnchangedOnFailure() {
        car.pickUp();
        assertThrows(IllegalArgumentException.class, () -> car.dropOff(999));
        assertEquals(Vehicle.Status.RENTED, car.status(), "failed drop-off must not free the car");
        assertEquals(1_000, car.mileage());
    }

    @Test
    void sameMileageOnReturnIsAllowed() {       // boundary: car picked up but never driven
        car.pickUp();
        car.dropOff(1_000);
        assertEquals(Vehicle.Status.AVAILABLE, car.status());
    }

    @Test
    void carInMaintenanceCannotBePickedUp() {
        car.sendToMaintenance();
        assertThrows(IllegalStateException.class, car::pickUp);
    }

    @Test
    void identityIsTheLicensePlate() {
        Vehicle samePlate = new Vehicle("ABC-123", "Honda", "Civic", VehicleType.SUV, 5);
        assertEquals(car, samePlate);
        assertEquals(car.hashCode(), samePlate.hashCode());
        assertNotEquals(car, new Vehicle("XYZ-999", "Toyota", "Camry", VehicleType.SEDAN, 1_000));
    }
}

