package com.fireway.backend.modules.cctv.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fireway.backend.modules.cctv.application.port.CctvReadingRepository;
import com.fireway.backend.modules.cctv.domain.CctvReading;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * cctv_readings 실제 JDBC 조회. verdict/source_meta JSON 컬럼은 Jackson 으로 lazy 파싱.
 *
 * verdict 는 두 shape 다 흡수한다:
 *  - {"pump-3.5":"PASS","pump-8":"FAIL"}     — AI 파이프라인 실제 저장
 *  - {"status":"PASS"}                        — V3_1 mock fixture
 * source_meta 는 AI 인계 스펙: {lat,lon,s3_media:{bucket,key,content_type},measurement_status,
 * measurement_failure:{attempted_at}}. 없는 값은 null 반환 — 서비스 계층에서 fallback.
 */
@Component
public class CctvReadingRepositoryAdapter implements CctvReadingRepository {

    private static final Logger log = LoggerFactory.getLogger(CctvReadingRepositoryAdapter.class);

    private static final String SELECT_COLS = """
            cctv_id, still_public_url, wall_width_m, obstacle_width_m, effective_width_m,
            verdict, confidence, measured_at, source_meta
            """;
    private static final String FIND_ONE = "SELECT " + SELECT_COLS + " FROM cctv_readings WHERE cctv_id = ?";
    private static final String FIND_ALL = "SELECT " + SELECT_COLS + " FROM cctv_readings ORDER BY cctv_id";

    private static final TypeReference<Map<String, String>> STRING_MAP =
            new TypeReference<Map<String, String>>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public CctvReadingRepositoryAdapter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Optional<CctvReading> findById(String id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(FIND_ONE, this::mapRow, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<CctvReading> findAll() {
        return jdbc.query(FIND_ALL, this::mapRow);
    }

    private CctvReading mapRow(ResultSet rs, int rowNum) throws SQLException {
        String cctvId = rs.getString("cctv_id");
        Double wall = nullableDouble(rs, "wall_width_m");
        Double obstacle = nullableDouble(rs, "obstacle_width_m");
        Double effective = nullableDouble(rs, "effective_width_m");
        Double confidence = nullableDouble(rs, "confidence");
        LocalDateTime measuredAt = toLdt(rs.getTimestamp("measured_at"));

        Map<String, String> verdict = parseVerdict(rs.getString("verdict"));
        SourceMeta sm = parseSourceMeta(rs.getString("source_meta"));

        return new CctvReading(
                cctvId, effective, wall, obstacle, verdict, confidence, measuredAt,
                sm.lastAttemptedAt(), sm.lat(), sm.lon(),
                sm.s3Key(), sm.contentType(), sm.measurementStatus(),
                rs.getString("still_public_url"));
    }

    private Map<String, String> parseVerdict(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, String> raw = mapper.readValue(json, STRING_MAP);
            if (raw == null) return Map.of();
            // pump-* 키가 있으면 그대로. 옛 fixture 는 {"status":"PASS"} — 모든 차종에 같은 값 복사하지 않음.
            // status 만 있으면 status 그대로 두고 서비스가 필요시 fallback.
            Map<String, String> normalized = new LinkedHashMap<>();
            raw.forEach((k, v) -> { if (v != null) normalized.put(k, v); });
            return normalized;
        } catch (Exception e) {
            log.warn("verdict JSON 파싱 실패 · 빈 map 반환: {}", e.getMessage());
            return Map.of();
        }
    }

    private SourceMeta parseSourceMeta(String json) {
        if (json == null || json.isBlank()) return SourceMeta.EMPTY;
        try {
            JsonNode root = mapper.readTree(json);
            Double lat = doubleAt(root, "lat");
            Double lon = doubleAt(root, "lon");
            String status = textAt(root, "measurement_status");
            LocalDateTime lastAttempt = null;
            JsonNode fail = root.path("measurement_failure").path("attempted_at");
            if (fail != null && fail.isTextual()) {
                lastAttempt = parseLdt(fail.asText());
            }
            String s3Key = null;
            String contentType = null;
            JsonNode s3 = root.path("s3_media");
            if (s3 != null && s3.isObject()) {
                s3Key = textAt(s3, "key");
                contentType = textAt(s3, "content_type");
            }
            return new SourceMeta(lat, lon, s3Key, contentType, status, lastAttempt);
        } catch (Exception e) {
            log.warn("source_meta JSON 파싱 실패: {}", e.getMessage());
            return SourceMeta.EMPTY;
        }
    }

    private static Double doubleAt(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v != null && v.isNumber()) ? v.asDouble() : null;
    }

    private static String textAt(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v != null && v.isTextual()) ? v.asText() : null;
    }

    private static Double nullableDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private static LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static LocalDateTime parseLdt(String iso) {
        try {
            // AI 인계는 KST ISO8601 (offset 포함) — offset 무시하고 로컬 벽시계로 저장.
            return java.time.OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception ignore) {
            try { return LocalDateTime.parse(iso); } catch (Exception e) { return null; }
        }
    }

    private record SourceMeta(Double lat, Double lon, String s3Key, String contentType,
            String measurementStatus, LocalDateTime lastAttemptedAt) {
        static final SourceMeta EMPTY = new SourceMeta(null, null, null, null, null, null);
    }
}
