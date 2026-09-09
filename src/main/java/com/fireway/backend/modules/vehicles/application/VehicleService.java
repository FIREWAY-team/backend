package com.fireway.backend.modules.vehicles.application;

import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {
    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) { this.repository = repository; }

    public Optional<Vehicle> findById(String id) {
        return repository.findById(id).or(() -> mock().stream().filter(v -> v.vehicleId().equals(id)).findFirst());
    }

    public List<Vehicle> mock() {
        return List.of(new Vehicle("pump-3.5", "펌프차 3.5톤", 2.1, 2.8, 6.5, 3.5, 8),
                new Vehicle("pump-8", "펌프차 8톤", 2.5, 3.2, 8.2, 8, 10.5));
    }
}
