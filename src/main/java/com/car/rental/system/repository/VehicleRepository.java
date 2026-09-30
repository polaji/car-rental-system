package com.car.rental.system.repository;

import com.car.rental.system.model.Vehicle;
import com.car.rental.system.model.VehicleType;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    void save(Vehicle vehicle);
    Optional<Vehicle> findByPlate(String plate);
    List<Vehicle> findByType(VehicleType type);
    List<Vehicle> findAll();
}
