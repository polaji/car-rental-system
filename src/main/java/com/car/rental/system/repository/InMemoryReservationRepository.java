package com.car.rental.system.repository;

import com.car.rental.system.model.Reservation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public void save(Reservation reservation) {
        reservations.put(reservation.id(), reservation);
    }

    @Override
    public Optional<Reservation> findById(String id) {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public List<Reservation> findByVehicle(String plate) {
        return reservations.values().stream()
                                    .filter(r -> r.vehicle().plate().equals(plate))
                                    .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByCustomer(String customerId) {
        return reservations.values().stream()
                                    .filter(r -> r.customer().id().equals(customerId))
                                    .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(reservations.values()));
    }
}

