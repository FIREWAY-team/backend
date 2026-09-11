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
        // 예선 제출서식 차량 제원과 정합 유지 — 실 성남소방서 배치 정보 확보 후 교체 예정.
        return List.of(new Vehicle("pump-3.5", "소형펌프차", 2.3, 3.0, 7.0, 3.5, 6.5),
                new Vehicle("pump-8", "중형펌프차", 2.5, 3.3, 8.0, 8.0, 8.0),
                new Vehicle("aerial-25", "25m 굴절차", 2.5, 3.6, 11.5, 18.0, 10.5));
    }
}
