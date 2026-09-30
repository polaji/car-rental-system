package com.car.rental.system.service;

import com.car.rental.system.model.DateRange;
import com.car.rental.system.model.Reservation;
import com.car.rental.system.model.Vehicle;
import com.car.rental.system.model.VehicleType;
import com.car.rental.system.model.*;
import com.car.rental.system.pricing.PricingStrategy;
import com.car.rental.system.repository.ReservationRepository;
import com.car.rental.system.repository.VehicleRepository;
import com.car.rental.system.service.RentalException.NoAvailabilityException;
import com.car.rental.system.service.RentalException.NotFoundException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Application service: orchestrates repositories + pricing.
 * Concurrency: two customers must never get the same car for overlapping dates. The check-then-act
 * (is it free? -> book it) is made atomic per vehicle with a per-plate lock object.
 */
public class RentalService {

    private final VehicleRepository vehicles;
    private final ReservationRepository reservations;
    private final PricingStrategy pricing;
    private final Map<String, Object> vehicleLocks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    public RentalService(VehicleRepository vehicles, ReservationRepository reservations, PricingStrategy pricing) {
        this.vehicles = vehicles;
        this.reservations = reservations;
        this.pricing = pricing;
    }

    /**
     * @param type
     * @param period
     * @return
     */
    public List<Vehicle> findAvailable(VehicleType type, DateRange period) {
        return vehicles.findByType(type)
                .stream()
                .filter(v -> v.status() != Vehicle.Status.MAINTENANCE)
                .filter(v -> isFree(v, period))
                .collect(Collectors.toList());
    }

    /**
     * @param type
     * @param period
     * @param customer
     * @return
     */
    public BigDecimal quote(VehicleType type, DateRange period, Customer customer) {
        return pricing.price(type, period, customer);
    }

    /**
     * @param customer
     * @param type
     * @param period
     * @return
     */
    public Reservation reserve(Customer customer, VehicleType type, DateRange period) {
        for (Vehicle vehicle : vehicles.findByType(type)) {
            if (vehicle.status() == Vehicle.Status.MAINTENANCE) continue;
            Object lock = vehicleLocks.computeIfAbsent(vehicle.plate(), p -> new Object());
            synchronized (lock) {                                  // check-then-act must be atomic per car
                if (isFree(vehicle, period)) {
                    Reservation r = new Reservation("R" + sequence.incrementAndGet(), customer, vehicle, period,
                            pricing.price(type, period, customer));
                    reservations.save(r);
                    return r;
                }
            }
        }
        throw new NoAvailabilityException("No " + type + " available " + period.start() + " to " + period.end());
    }

    /**
     * @param reservationId
     * @return
     */
    public Reservation cancel(String reservationId) {
        Reservation r = get(reservationId);
        r.transitionTo(Reservation.Status.CANCELLED);
        return r;
    }

    /**
     * @param reservationId
     * @return
     */
    public Reservation pickUp(String reservationId) {
        Reservation r = get(reservationId);
        synchronized (r) {
            r.checkTransition(Reservation.Status.ACTIVE);
            r.vehicle().pickUp();
            r.transitionTo(Reservation.Status.ACTIVE);
        }
        return r;
    }

    /**
     * @param reservationId
     * @param odometer
     * @return
     */
    public Reservation returnVehicle(String reservationId, int odometer) {
        Reservation r = get(reservationId);
        synchronized (r) {
            r.checkTransition(Reservation.Status.COMPLETED);   // e.g. a RESERVED booking must not "return" a car
            r.vehicle().dropOff(odometer);                     // that is rented under a different reservation
            r.transitionTo(Reservation.Status.COMPLETED);
        }
        return r;
    }

    /**
     * @param customerId
     * @return
     */
    public List<Reservation> history(String customerId) {
        return reservations.findByCustomer(customerId)
                           .stream()
                           .sorted(Comparator.comparing((Reservation r) -> r.period().start()))
                           .collect(Collectors.toList());
    }

    /**
     * @return
     */
    public Map<VehicleType, BigDecimal> revenueByType() {
        return reservations.findAll()
                           .stream()
                           .filter(r -> r.status() == Reservation.Status.COMPLETED || r.status() == Reservation.Status.ACTIVE)
                           .collect(Collectors.groupingBy(r -> r.vehicle().type(), () -> new EnumMap<>(VehicleType.class),
                                    Collectors.reducing(BigDecimal.ZERO, Reservation::price, BigDecimal::add)));
    }

    /**
     * @param v
     * @param period
     * @return
     */
    private boolean isFree(Vehicle v, DateRange period) {
        return reservations.findByVehicle(v.plate()).stream().noneMatch(r -> r.blocks(period));
    }

    /**
     * @param id
     * @return
     */
    private Reservation get(String id) {
        return reservations.findById(id).orElseThrow(() -> new NotFoundException("Reservation " + id + " not found"));
    }
}

