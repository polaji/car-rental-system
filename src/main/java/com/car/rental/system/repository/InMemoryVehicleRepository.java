package com.car.rental.system.repository;

import com.car.rental.system.model.Vehicle;
import com.car.rental.system.model.VehicleType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryVehicleRepository implements VehicleRepository {

    private final Map<String, Vehicle> vehicles = new ConcurrentHashMap<>();

    @Override
    public void save(Vehicle vehicle) {
        vehicles.put(vehicle.plate(), vehicle);
    }

    @Override
    public Optional<Vehicle> findByPlate(String plate) {
        return Optional.ofNullable(vehicles.get(plate));
    }

    @Override
    public List<Vehicle> findByType(VehicleType type) {
        return vehicles.values()
                .stream()
                .filter(v -> v.type() == type)
                .sorted(Comparator.comparing(Vehicle::plate))       // deterministic order
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public List<Vehicle> findAll() {
        return vehicles.values()
                .stream()
                .sorted(Comparator.comparing(Vehicle::plate))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }
}
