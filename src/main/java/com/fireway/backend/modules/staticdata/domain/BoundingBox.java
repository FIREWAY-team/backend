package com.fireway.backend.modules.staticdata.domain;
/**
 * 조회 범위. routing 모듈에도 같은 이름의 타입이 있지만 일부러 공유하지 않는다 —
 * 모듈 간 도메인을 직접 물리면 경계가 무너진다. 값만 받고 변환은 어댑터가 한다.
 */
public record BoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
    public BoundingBox {
        if (minLat > maxLat || minLon > maxLon) {
            throw new IllegalArgumentException(
                    "bbox 의 최소값이 최대값보다 큽니다: lat %f~%f, lon %f~%f"
                            .formatted(minLat, maxLat, minLon, maxLon));
        }
    }

    /** 두 점을 감싸고 여유를 둔 범위. 경로 주변 구간을 뽑을 때 쓴다. */
    public static BoundingBox around(Coordinate a, Coordinate b, double paddingM) {
        double latPad = paddingM / 111_320.0;
        double maxAbsLat = Math.max(Math.abs(a.lat()), Math.abs(b.lat()));
        double lonPad = latPad / Math.max(1e-6, Math.cos(Math.toRadians(maxAbsLat)));
        return new BoundingBox(
                Math.max(-90, Math.min(a.lat(), b.lat()) - latPad),
                Math.max(-180, Math.min(a.lon(), b.lon()) - lonPad),
                Math.min(90, Math.max(a.lat(), b.lat()) + latPad),
                Math.min(180, Math.max(a.lon(), b.lon()) + lonPad));
    }

    /** 이 좌표가 범위 안에 있는지. */
    public boolean contains(Coordinate c) {
        return c.lat() >= minLat && c.lat() <= maxLat && c.lon() >= minLon && c.lon() <= maxLon;
    }

    /**
     * MySQL 이 읽을 WKT. no_go_areas.geom 과 같은 축 순서(long-lat)로 쓴다.
     * 여기서 순서를 틀리면 조회가 조용히 0건이 된다.
     */
    public String toPolygonWkt() {
        return "POLYGON((%1$.7f %2$.7f,%3$.7f %2$.7f,%3$.7f %4$.7f,%1$.7f %4$.7f,%1$.7f %2$.7f))"
                .formatted(minLon, minLat, maxLon, maxLat);
    }
}
