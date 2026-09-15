package com.fireway.backend.modules.staticdata.domain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

class BoundingBoxTest {
    // 성남 중원구. 경도 127, 위도 37 이다 — 뒤집히면 바로 보인다.
    private static final Coordinate 은행1동 = new Coordinate(37.4381, 127.1422);
    private static final Coordinate 상대원 = new Coordinate(37.4311, 127.1642);

    @Test void 두_점을_감싸고_여유를_둔다() {
        BoundingBox box = BoundingBox.around(은행1동, 상대원, 300);
        assertThat(box.contains(은행1동)).isTrue();
        assertThat(box.contains(상대원)).isTrue();
        assertThat(box.minLat()).isLessThan(37.4311);
        assertThat(box.maxLat()).isGreaterThan(37.4381);
        assertThat(box.maxLat() - 37.4381).isCloseTo(0.0027, Offset.offset(0.0003));  // 300m ≈ 0.0027°
    }

    // 위도가 높을수록 같은 미터가 더 큰 경도 각도가 된다. 경도 여유가 위도 여유보다 커야 한다.
    @Test void 경도_여유는_위도_보정을_받는다() {
        BoundingBox box = BoundingBox.around(은행1동, 은행1동, 300);
        assertThat(box.maxLon() - 127.1422).isGreaterThan(box.maxLat() - 37.4381);
    }

    @Test void 범위_밖_좌표는_걸러진다() {
        assertThat(BoundingBox.around(은행1동, 은행1동, 100).contains(상대원)).isFalse();
    }

    /**
     * 이 테스트가 이 클래스의 존재 이유다. no_go_areas.geom 은 axis-order=long-lat 로
     * 넣고 읽는다. bbox WKT 가 lat-lon 으로 나가면 MySQL 이 에러 없이 0건을 돌려준다.
     */
    @Test void WKT_는_경도_위도_순서로_쓴다() {
        String wkt = new BoundingBox(37.43, 127.14, 37.44, 127.15).toPolygonWkt();
        assertThat(wkt).startsWith("POLYGON((127.1400000 37.4300000,");
        String[] xy = wkt.substring(wkt.indexOf("((") + 2, wkt.indexOf(",")).split(" ");
        assertThat(Double.parseDouble(xy[0])).isBetween(126.0, 128.0);   // 경도가 앞
        assertThat(Double.parseDouble(xy[1])).isBetween(37.0, 38.0);     // 위도가 뒤
    }

    @Test void WKT_는_닫힌_사각형이다() {
        String wkt = new BoundingBox(37.43, 127.14, 37.44, 127.15).toPolygonWkt();
        String[] pts = wkt.substring(wkt.indexOf("((") + 2, wkt.indexOf("))")).split(",");
        assertThat(pts).hasSize(5);
        assertThat(pts[0]).isEqualTo(pts[4]);
    }

    @Test void 뒤집힌_범위는_거부한다() {
        assertThatThrownBy(() -> new BoundingBox(37.44, 127.14, 37.43, 127.15))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoundingBox(37.43, 127.15, 37.44, 127.14))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
