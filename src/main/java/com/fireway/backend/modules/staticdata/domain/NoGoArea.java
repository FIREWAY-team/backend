package com.fireway.backend.modules.staticdata.domain;
import java.util.List;
/**
 * 진입곤란 구간. 관내도에서 임포트한 분은 도로 중심선(LineString)이고,
 * V2_1 픽스처로 들어간 2건은 면(Polygon)이다.
 */
public record NoGoArea(long id, String extId, String dong, String reason, int layer,
                       String verificationStatus, String note,
                       GeometryType geometryType, List<Coordinate> path) {
    /** 그대로 써도 되는 구간. V2_3 verification_status 의 기본값이다. */
    public static final String OK = "ok";
    /** 위성 대조에서 실제 도로가 아닌 것으로 확인된 구간(재개발로 사라진 옛 골목 등). */
    public static final String UNVERIFIED = "unverified";

    /** 라우팅이 실제로 막아도 되는 구간인지. 지도 표시에는 unverified 도 내려간다. */
    public boolean routable() { return OK.equals(verificationStatus); }

    public enum GeometryType {
        LINE_STRING("LineString"), POLYGON("Polygon");
        private final String geoJsonName;
        GeometryType(String geoJsonName) { this.geoJsonName = geoJsonName; }
        /** GeoJSON 표기. HTTP 응답에 그대로 실린다. */
        public String geoJsonName() { return geoJsonName; }
    }
}
