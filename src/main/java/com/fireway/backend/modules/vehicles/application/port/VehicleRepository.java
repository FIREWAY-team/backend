package com.fireway.backend.modules.vehicles.application.port;

import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    List<Vehicle> findAll();
    Optional<Vehicle> findById(String id);
}
