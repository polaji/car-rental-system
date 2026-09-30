package com.car.rental.system.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Reservation {

    /** Lifecycle: RESERVED -> ACTIVE -> COMPLETED, or RESERVED -> CANCELLED. */
    public enum Status {
        RESERVED, ACTIVE, COMPLETED, CANCELLED;

        /** Is the move from this status to {@code next} allowed? */
        boolean canMoveTo(Status next) {
            switch (this) {
                case RESERVED:
                    return next == ACTIVE || next == CANCELLED;   // pick up, or cancel
                case ACTIVE:
                    return next == COMPLETED;                     // return the car
                default:
                    return false;                                 // COMPLETED and CANCELLED are final
            }
        }

        /** Does a booking in this status still hold the car for its dates? */
        public boolean isBlocking() {
            return this == RESERVED || this == ACTIVE;
        }
    }

    private final String id;
    private final Customer customer;
    private final Vehicle vehicle;
    private final DateRange period;
    private final BigDecimal price;
    private Status status = Status.RESERVED;

    public Reservation(String id, Customer customer, Vehicle vehicle, DateRange period, BigDecimal price) {
        this.id = Objects.requireNonNull(id);
        this.customer = Objects.requireNonNull(customer);
        this.vehicle = Objects.requireNonNull(vehicle);
        this.period = Objects.requireNonNull(period);
        this.price = Objects.requireNonNull(price);
    }

    public String id() { return id; }
    public Customer customer() { return customer; }
    public Vehicle vehicle() { return vehicle; }
    public DateRange period() { return period; }
    public BigDecimal price() { return price; }
    public synchronized Status status() { return status; }

    public synchronized boolean blocks(DateRange requested) {
        return status.isBlocking() && period.overlaps(requested);
    }

    /** Validates a transition without applying it, so callers can fail fast before touching other objects. */
    public synchronized void checkTransition(Status next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Reservation " + id + ": " + status + " -> " + next + " not allowed");
        }
    }

    public synchronized void transitionTo(Status next) {
        checkTransition(next);
        status = next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation)) return false;
        return id.equals(((Reservation) o).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return id + " " + customer.name() + " " + vehicle.plate() + " " + period.start() + ".." + period.end()
                + " $" + price + " " + status();
    }
}
