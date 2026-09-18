package com.fireway.backend.modules.staticdata.infrastructure;
import com.fireway.backend.modules.staticdata.application.port.NoGoAreaRepository;
import com.fireway.backend.modules.staticdata.domain.BoundingBox;
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
    // MBRIntersects 가 V2 의 SPATIAL INDEX 를 탄다. ST_Intersects 는 정밀 판정이라
    // 여기선 과하다 — bbox 로 후보만 줄이는 게 목적이다.
    private static final String FIND_ROUTABLE_WITHIN = SELECT + """
             WHERE verification_status = ?
               AND MBRIntersects(geom, ST_GeomFromText(?, 4326, 'axis-order=long-lat'))
             ORDER BY id
            """;
    // 지도 표시용이라 verification_status 로 거르지 않는다. 인덱스는 같은 SPATIAL 을 탄다.
    private static final String FIND_ALL_WITHIN = SELECT + """
             WHERE MBRIntersects(geom, ST_GeomFromText(?, 4326, 'axis-order=long-lat'))
             ORDER BY id
            """;

    private final JdbcTemplate jdbc;
    public NoGoAreaRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<NoGoArea> findAll() {
        return jdbc.query(FIND_ALL, NoGoAreaRepositoryAdapter::mapRow);
    }

    /** 포트의 기본 구현(메모리 필터) 대신 SQL 로 거른다. */
    @Override public List<NoGoArea> findRoutable() {
        return jdbc.query(FIND_ROUTABLE, NoGoAreaRepositoryAdapter::mapRow, NoGoArea.OK);
    }

    /** bbox 도 geom 과 같은 축 순서(long-lat)로 넘긴다. 틀리면 조용히 0건이 된다. */
    @Override public List<NoGoArea> findRoutableWithin(BoundingBox box) {
        return jdbc.query(FIND_ROUTABLE_WITHIN, NoGoAreaRepositoryAdapter::mapRow,
                NoGoArea.OK, box.toPolygonWkt());
    }

    /** 위와 같은 축 순서(long-lat)다. 상태 필터만 없다. */
    @Override public List<NoGoArea> findAllWithin(BoundingBox box) {
        return jdbc.query(FIND_ALL_WITHIN, NoGoAreaRepositoryAdapter::mapRow, box.toPolygonWkt());
    }

    private static NoGoArea mapRow(ResultSet rs, int rowNum) throws SQLException {
        String wkt = rs.getString("geom_wkt");
        return new NoGoArea(rs.getLong("id"), rs.getString("ext_id"), rs.getString("dong"),
                rs.getString("reason"), rs.getInt("layer"), rs.getString("verification_status"),
                rs.getString("note"), WktGeometry.typeOf(wkt), WktGeometry.path(wkt));
    }
}
