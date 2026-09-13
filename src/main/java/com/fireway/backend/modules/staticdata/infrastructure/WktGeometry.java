package com.fireway.backend.modules.staticdata.infrastructure;
import com.fireway.backend.modules.staticdata.domain.Coordinate;
import com.fireway.backend.modules.staticdata.domain.NoGoArea.GeometryType;
import com.fireway.backend.shared.exception.InfrastructureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/**
 * MySQL ST_AsText 결과를 도메인 좌표로 바꾼다.
 * no_go_areas 는 JPA 엔티티가 없는 읽기 전용 테이블이라 hibernate-spatial 을 끌어오지 않고 직접 파싱한다.
 */
final class WktGeometry {
    private WktGeometry() { }

    static GeometryType typeOf(String wkt) {
        String tag = wkt.substring(0, openParen(wkt)).trim().toUpperCase(Locale.ROOT);
        return switch (tag) {
            case "LINESTRING" -> GeometryType.LINE_STRING;
            case "POLYGON" -> GeometryType.POLYGON;
            default -> throw new InfrastructureException("no_go_areas.geom 에 예상 못 한 도형이다: " + tag);
        };
    }

    /** POLYGON 은 외곽 링만 쓴다. 진입곤란 구간에 구멍이 있는 도형은 들어오지 않는다. */
    static List<Coordinate> path(String wkt) {
        String body = wkt.substring(openParen(wkt) + 1, wkt.lastIndexOf(')')).trim();
        if (body.startsWith("(")) body = body.substring(1, body.indexOf(')'));
        List<Coordinate> path = new ArrayList<>();
        for (String point : body.split(",")) {
            String[] xy = point.trim().split("\s+");
            if (xy.length < 2) throw new InfrastructureException("좌표를 읽을 수 없다: " + point);
            // axis-order=long-lat 로 읽었으므로 앞이 경도다
            path.add(new Coordinate(Double.parseDouble(xy[1]), Double.parseDouble(xy[0])));
        }
        return path;
    }

    private static int openParen(String wkt) {
        int i = wkt == null ? -1 : wkt.indexOf('(');
        if (i < 0) throw new InfrastructureException("WKT 로 읽을 수 없다: " + wkt);
        return i;
    }
}
