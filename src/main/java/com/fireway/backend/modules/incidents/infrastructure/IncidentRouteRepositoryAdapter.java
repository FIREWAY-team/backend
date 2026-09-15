package com.fireway.backend.modules.incidents.infrastructure;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fireway.backend.modules.incidents.application.port.IncidentRouteRepository;
import com.fireway.backend.modules.incidents.domain.IncidentRoute;
import com.fireway.backend.shared.exception.InfrastructureException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component public class IncidentRouteRepositoryAdapter implements IncidentRouteRepository {
    private static final String COLUMNS = """
            id, incident_id, vehicle_id, rank_no, distance_m, eta_seconds, polyline,
            passable_for_vehicle, meets_golden_time, passable_prob, explanation,
            excluded_reasons, unlocked_by_cctv, planned_at
            """;
    private static final String DELETE = "DELETE FROM incident_routes WHERE incident_id = ?";
    private static final String INSERT = """
            INSERT INTO incident_routes
              (incident_id, vehicle_id, rank_no, distance_m, eta_seconds, polyline,
               passable_for_vehicle, meets_golden_time, passable_prob, explanation,
               excluded_reasons, unlocked_by_cctv)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND = "SELECT " + COLUMNS
            + " FROM incident_routes WHERE incident_id = ? ORDER BY rank_no";

    private static final TypeReference<List<IncidentRoute.Blocked>> BLOCKED_LIST = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public IncidentRouteRepositoryAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** 다시 계산하면 이전 결과를 통째로 대체한다. 차량이 바뀌면 판정도 통째로 바뀐다. */
    @Override public void replaceAll(long incidentId, List<IncidentRoute> routes) {
        jdbc.update(DELETE, incidentId);
        for (IncidentRoute r : routes) {
            jdbc.update(INSERT, incidentId, r.vehicleId(), r.rank(), r.distanceM(), r.etaSeconds(),
                    r.polyline(), r.passableForVehicle(), r.meetsGoldenTime(), r.passableProb(),
                    r.explanation(), write(r.excludedReasons()), write(r.unlockedByCctv()));
        }
    }

    @Override public List<IncidentRoute> findByIncidentId(long incidentId) {
        return jdbc.query(FIND, this::mapRow, incidentId);
    }

    private IncidentRoute mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp planned = rs.getTimestamp("planned_at");
        return new IncidentRoute(rs.getLong("id"), rs.getLong("incident_id"),
                rs.getString("vehicle_id"), rs.getInt("rank_no"),
                nullableInt(rs, "distance_m"), nullableInt(rs, "eta_seconds"),
                rs.getString("polyline"), rs.getBoolean("passable_for_vehicle"),
                rs.getBoolean("meets_golden_time"), nullableDouble(rs, "passable_prob"),
                rs.getString("explanation"),
                read(rs.getString("excluded_reasons"), BLOCKED_LIST),
                read(rs.getString("unlocked_by_cctv"), STRING_LIST),
                planned == null ? null : planned.toLocalDateTime());
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }
    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }

    private String write(Object value) {
        if (value == null) return null;
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new InfrastructureException("경로 근거를 JSON 으로 쓰지 못했습니다: " + e.getMessage());
        }
    }
    private <T> List<T> read(String raw, TypeReference<List<T>> type) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return json.readValue(raw, type);
        } catch (Exception e) {
            throw new InfrastructureException("경로 근거를 읽지 못했습니다: " + e.getMessage());
        }
    }
}
