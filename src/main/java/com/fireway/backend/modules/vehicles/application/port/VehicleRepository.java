package com.fireway.backend.modules.vehicles.application.port;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.List;
public interface VehicleRepository { List<Vehicle> findAll(); }
