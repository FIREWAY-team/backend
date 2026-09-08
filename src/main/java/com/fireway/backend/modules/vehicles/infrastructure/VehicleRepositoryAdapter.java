package com.fireway.backend.modules.vehicles.infrastructure;
import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.List;
import org.springframework.stereotype.Component;
@Component public class VehicleRepositoryAdapter implements VehicleRepository { public List<Vehicle> findAll() { return List.of(); } }
