package com.car.rental.system.repository;

import com.car.rental.system.model.Reservation;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    void save(Reservation reservation);
    Optional<Reservation> findById(String id);
    List<Reservation> findByVehicle(String plate);
    List<Reservation> findByCustomer(String customerId);
    List<Reservation> findAll();
}
