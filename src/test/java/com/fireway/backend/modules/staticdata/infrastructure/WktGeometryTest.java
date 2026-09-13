package com.fireway.backend.modules.staticdata.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fireway.backend.modules.staticdata.domain.Coordinate;
import com.fireway.backend.modules.staticdata.domain.NoGoArea.GeometryType;
import com.fireway.backend.shared.exception.InfrastructureException;
import java.util.List;
import org.junit.jupiter.api.Test;

class WktGeometryTest {
    // ST_AsText(geom, 'axis-order=long-lat') 가 내놓는 모양. 성남은 경도 127, 위도 37이다.
    private static final String LINE = "LINESTRING(127.142 37.438,127.143 37.439)";
    // V2_1 픽스처로 들어간 기존 2건
    private static final String POLYGON =
            "POLYGON((127.142 37.438,127.143 37.438,127.144 37.439,127.142 37.438))";

    @Test void 도형_종류를_구분한다() {
        assertThat(WktGeometry.typeOf(LINE)).isEqualTo(GeometryType.LINE_STRING);
        assertThat(WktGeometry.typeOf(POLYGON)).isEqualTo(GeometryType.POLYGON);
    }

    @Test void LineString_좌표를_축_순서를_지켜_읽는다() {
        assertThat(WktGeometry.path(LINE))
                .containsExactly(new Coordinate(37.438, 127.142), new Coordinate(37.439, 127.143));
    }

    // 위도와 경도를 뒤집으면 좌표가 성남이 아니라 중국 어딘가가 된다. 조용히 지나가면 안 되는 실수라 따로 잡는다.
    @Test void 위경도를_뒤집지_않는다() {
        Coordinate first = WktGeometry.path(LINE).get(0);
        assertThat(first.lat()).isBetween(37.2, 37.6);
        assertThat(first.lon()).isBetween(126.8, 127.4);
    }

    @Test void Polygon_은_외곽_링만_읽는다() {
        List<Coordinate> path = WktGeometry.path(POLYGON);
        assertThat(path).hasSize(4);
        assertThat(path.get(0)).isEqualTo(path.get(3));   // 닫힌 링
    }

    @Test void 예상_못_한_도형은_실패시킨다() {
        assertThatThrownBy(() -> WktGeometry.typeOf("POINT(127.142 37.438)"))
                .isInstanceOf(InfrastructureException.class);
        assertThatThrownBy(() -> WktGeometry.typeOf("쓰레기"))
                .isInstanceOf(InfrastructureException.class);
    }
}
