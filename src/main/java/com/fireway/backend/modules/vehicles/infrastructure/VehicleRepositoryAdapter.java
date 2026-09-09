package com.fireway.backend.modules.vehicles.infrastructure;

import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class VehicleRepositoryAdapter implements VehicleRepository {
    private static final String COLUMNS = "vehicle_id, name, width_m, height_m, length_m, weight_ton, turning_radius_m";
    private static final RowMapper<Vehicle> MAPPER = (rs, row) -> new Vehicle(rs.getString("vehicle_id"),
            rs.getString("name"), rs.getDouble("width_m"), rs.getDouble("height_m"), rs.getDouble("length_m"),
            rs.getDouble("weight_ton"), rs.getDouble("turning_radius_m"));
    private final JdbcTemplate jdbc;

    public VehicleRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Vehicle> findAll() { return jdbc.query("SELECT " + COLUMNS + " FROM vehicles", MAPPER); }

    @Override
    public Optional<Vehicle> findById(String id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM vehicles WHERE vehicle_id = ?", MAPPER, id).stream().findFirst();
    }
}
