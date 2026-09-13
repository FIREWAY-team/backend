package com.fireway.backend.modules.staticdata.infrastructure;
import com.fireway.backend.modules.staticdata.application.port.NoGoAreaRepository;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component public class NoGoAreaRepositoryAdapter implements NoGoAreaRepository {
    // 넣을 때 axis-order=long-lat 로 명시했으니 읽을 때도 명시한다.
    // 빼면 SRID 4326 정의대로 lat-lon 으로 나와서 좌표가 조용히 뒤집힌다.
    private static final String SELECT = """
            SELECT id, ext_id, dong, reason, layer, verification_status, note,
                   ST_AsText(geom, 'axis-order=long-lat') AS geom_wkt
            FROM no_go_areas
            """;
    private static final String FIND_ALL = SELECT + " ORDER BY id";
    // V2_3 의 ix_no_go_areas_verification 을 탄다.
    private static final String FIND_ROUTABLE =
            SELECT + " WHERE verification_status = ? ORDER BY id";

    private final JdbcTemplate jdbc;
    public NoGoAreaRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<NoGoArea> findAll() {
        return jdbc.query(FIND_ALL, NoGoAreaRepositoryAdapter::mapRow);
    }

    /** 포트의 기본 구현(메모리 필터) 대신 SQL 로 거른다. */
    @Override public List<NoGoArea> findRoutable() {
        return jdbc.query(FIND_ROUTABLE, NoGoAreaRepositoryAdapter::mapRow, NoGoArea.OK);
    }

    private static NoGoArea mapRow(ResultSet rs, int rowNum) throws SQLException {
        String wkt = rs.getString("geom_wkt");
        return new NoGoArea(rs.getLong("id"), rs.getString("ext_id"), rs.getString("dong"),
                rs.getString("reason"), rs.getInt("layer"), rs.getString("verification_status"),
                rs.getString("note"), WktGeometry.typeOf(wkt), WktGeometry.path(wkt));
    }
}
