package com.fireway.backend.modules.incidents.infrastructure;
import com.fireway.backend.modules.incidents.application.port.IncidentAttachmentRepository;
import com.fireway.backend.modules.incidents.domain.IncidentAttachment;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component public class IncidentAttachmentRepositoryAdapter implements IncidentAttachmentRepository {
    private static final String INSERT = """
            INSERT INTO incident_attachments (incident_id, file_key, content_type, size_bytes, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;
    // ix_incident_attachments_incident 가 필터와 정렬을 함께 받는다.
    private static final String FIND = """
            SELECT id, incident_id, file_key, content_type, size_bytes, created_at
            FROM incident_attachments WHERE incident_id = ? ORDER BY id
            """;

    private final JdbcTemplate jdbc;
    public IncidentAttachmentRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public long insert(IncidentAttachment a) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement(INSERT, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a.incidentId());
            ps.setString(2, a.fileKey());
            ps.setString(3, a.contentType());
            ps.setLong(4, a.sizeBytes());
            ps.setTimestamp(5, Timestamp.valueOf(a.createdAt()));
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("첨부 후 PK 를 받지 못했습니다");
        return key.longValue();
    }

    @Override public List<IncidentAttachment> findByIncidentId(long incidentId) {
        return jdbc.query(FIND, IncidentAttachmentRepositoryAdapter::mapRow, incidentId);
    }

    private static IncidentAttachment mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new IncidentAttachment(rs.getLong("id"), rs.getLong("incident_id"),
                rs.getString("file_key"), rs.getString("content_type"), rs.getLong("size_bytes"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
