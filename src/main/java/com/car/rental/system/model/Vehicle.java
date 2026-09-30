package com.car.rental.system.model;

import java.util.Objects;

/**
 * A physical car in the fleet. Identity = license plate (business key).
 * Mutable state (mileage, status) is guarded by synchronized methods because the same
 * Vehicle object can be touched by several booking threads.
 */
public class Vehicle {

    public enum Status { AVAILABLE, RENTED, MAINTENANCE }

    private final String plate;
    private final String make;
    private final String model;
    private final VehicleType type;
    private int mileage;
    private Status status = Status.AVAILABLE;

    public Vehicle(String plate, String make, String model, VehicleType type, int mileage) {
        if (plate == null || plate.trim().isEmpty()) throw new IllegalArgumentException("plate required");
        this.plate = plate;
        this.make = Objects.requireNonNull(make);
        this.model = Objects.requireNonNull(model);
        this.type = Objects.requireNonNull(type);
        if (mileage < 0) throw new IllegalArgumentException("mileage < 0");
        this.mileage = mileage;
    }

    public String plate() { return plate; }
    public String make() { return make; }
    public String model() { return model; }
    public VehicleType type() { return type; }
    public synchronized int mileage() { return mileage; }
    public synchronized Status status() { return status; }

    public synchronized void pickUp() {
        if (status != Status.AVAILABLE) throw new IllegalStateException(plate + " is " + status);
        status = Status.RENTED;
    }

    public synchronized void dropOff(int newMileage) {
        if (status != Status.RENTED) throw new IllegalStateException(plate + " is not rented");
        if (newMileage < mileage) throw new IllegalArgumentException("mileage cannot go backwards");
        mileage = newMileage;
        status = Status.AVAILABLE;
    }

    public synchronized void sendToMaintenance() { status = Status.MAINTENANCE; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle)) return false;
        return plate.equals(((Vehicle) o).plate);
    }

    @Override
    public int hashCode() { return plate.hashCode(); }

    @Override
    public String toString() { return type + " " + make + " " + model + " [" + plate + "]"; }
}
