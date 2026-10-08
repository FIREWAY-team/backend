package com.fireway.backend.modules.incidents.infrastructure;
import com.fireway.backend.modules.incidents.application.port.IncidentRepository;
import com.fireway.backend.modules.incidents.domain.Incident;
import com.fireway.backend.modules.incidents.domain.IncidentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component public class IncidentRepositoryAdapter implements IncidentRepository {
    private static final String COLUMNS =
            "id, incident_no, status, address, lat, lon, summary, received_at, closed_at, "
            + "reporter_name, reporter_phone, severity, estimated_area_m2, building_type, casualties_reported, notes";
    private static final String INSERT = """
            INSERT INTO incidents (incident_no, status, address, lat, lon, summary, received_at,
                                   reporter_name, reporter_phone, severity, estimated_area_m2,
                                   building_type, casualties_reported, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_STATUS =
            "UPDATE incidents SET status = ?, closed_at = ? WHERE id = ? AND status = ?";
    // ix_incidents_status_received 가 상태 필터와 정렬을 함께 받는다.
    private static final String FIND_BY_STATUS =
            "SELECT " + COLUMNS + " FROM incidents WHERE status = ? ORDER BY received_at DESC, id DESC";
    private static final String FIND_ALL =
            "SELECT " + COLUMNS + " FROM incidents ORDER BY received_at DESC, id DESC";
    private static final String FIND_BY_NO =
            "SELECT " + COLUMNS + " FROM incidents WHERE incident_no = ?";
    // 건수가 아니라 이미 쓴 번호의 최댓값을 본다. COUNT(*) 로 세면 행이 하나라도 지워졌을 때
    // 발급이 뒤로 돌아가 같은 번호를 다시 내주고, 유니크 키에 계속 막혀 그날 접수가 통째로 멈춘다.
    // LIKE 는 앞자리가 고정이라 uk_incidents_no 를 범위 스캔으로 탄다.
    private static final String MAX_SEQ_ON_DATE = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(incident_no, %d) AS UNSIGNED)), 0)
              FROM incidents WHERE incident_no LIKE ?
            """.formatted(Incident.NUMBER_PREFIX_LENGTH + 1);

    private final JdbcTemplate jdbc;
    public IncidentRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public long insert(Incident incident) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement(INSERT, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, incident.incidentNo());
            ps.setString(2, incident.status().name());
            ps.setString(3, incident.address());
            ps.setDouble(4, incident.lat());
            ps.setDouble(5, incident.lon());
            ps.setString(6, incident.summary());
            ps.setTimestamp(7, Timestamp.valueOf(incident.receivedAt()));
            Incident.Intake in = incident.intake();
            ps.setString(8, in.reporterName());
            ps.setString(9, in.reporterPhone());
            ps.setString(10, in.severity());
            ps.setObject(11, in.estimatedAreaM2(), java.sql.Types.INTEGER);
            ps.setString(12, in.buildingType());
            ps.setBoolean(13, in.casualtiesReported());
            ps.setString(14, in.notes());
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("접수 후 PK 를 받지 못했습니다");
        return key.longValue();
    }

    @Override public List<Incident> findAll(IncidentStatus status) {
        return status == null
                ? jdbc.query(FIND_ALL, IncidentRepositoryAdapter::mapRow)
                : jdbc.query(FIND_BY_STATUS, IncidentRepositoryAdapter::mapRow, status.name());
    }

    @Override public Optional<Incident> findByNo(String incidentNo) {
        return jdbc.query(FIND_BY_NO, IncidentRepositoryAdapter::mapRow, incidentNo).stream().findFirst();
    }

    @Override public boolean updateStatus(Incident moved, IncidentStatus expected) {
        Timestamp closedAt = moved.closedAt() == null ? null : Timestamp.valueOf(moved.closedAt());
        return jdbc.update(UPDATE_STATUS, moved.status().name(), closedAt, moved.id(), expected.name()) == 1;
    }

    @Override public int lastSequenceOn(LocalDate date) {
        Integer n = jdbc.queryForObject(MAX_SEQ_ON_DATE, Integer.class, Incident.numberPrefix(date) + "%");
        return n == null ? 0 : n;
    }

    private static Incident mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Incident(rs.getLong("id"), rs.getString("incident_no"),
                IncidentStatus.valueOf(rs.getString("status")), rs.getString("address"),
                rs.getDouble("lat"), rs.getDouble("lon"), rs.getString("summary"),
                at(rs.getTimestamp("received_at")), at(rs.getTimestamp("closed_at")),
                new Incident.Intake(rs.getString("reporter_name"), rs.getString("reporter_phone"),
                        rs.getString("severity"), rs.getObject("estimated_area_m2", Integer.class),
                        rs.getString("building_type"), rs.getBoolean("casualties_reported"), rs.getString("notes")));
    }
    private static LocalDateTime at(Timestamp t) { return t == null ? null : t.toLocalDateTime(); }
}
