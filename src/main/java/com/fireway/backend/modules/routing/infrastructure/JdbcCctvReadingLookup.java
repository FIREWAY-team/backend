package com.fireway.backend.modules.routing.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fireway.backend.modules.routing.application.port.CctvReadingLookup;
import com.fireway.backend.modules.routing.domain.CctvVerdict;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Uses cctv_readings.edge_id as the CCTV-to-no-go polygon mapping. */
@Primary
@Component
public class JdbcCctvReadingLookup implements CctvReadingLookup {

    private static final Logger log = LoggerFactory.getLogger(JdbcCctvReadingLookup.class);
    private static final String FIND_LATEST = """
            SELECT cctv_id, effective_width_m, verdict, confidence
            FROM cctv_readings
            WHERE edge_id = ?
            ORDER BY measured_at DESC, cctv_id DESC
            LIMIT 1
            """;
    private static final TypeReference<Map<String, String>> STRING_MAP =
            new TypeReference<Map<String, String>>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JdbcCctvReadingLookup(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Optional<CctvVerdict> forPolygon(String polygonId) {
        if (polygonId == null || polygonId.isBlank()) return Optional.empty();
        return jdbc.query(FIND_LATEST, this::mapRow, polygonId).stream().findFirst();
    }

    private CctvVerdict mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CctvVerdict(
                rs.getString("cctv_id"),
                nullableDouble(rs, "effective_width_m"),
                parseVerdict(rs.getString("verdict")),
                nullableDouble(rs, "confidence"));
    }

    Map<String, String> parseVerdict(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, String> raw = mapper.readValue(json, STRING_MAP);
            if (raw == null) return Map.of();
            Map<String, String> result = new LinkedHashMap<>();
            raw.forEach((key, value) -> {
                if (value != null) result.put(key, value);
            });
            return result;
        } catch (Exception e) {
            log.warn("CCTV verdict JSON 파싱 실패 · polygon unlock 생략: {}", e.getMessage());
            return Map.of();
        }
    }

    private static double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? 0.0 : value;
    }
}
