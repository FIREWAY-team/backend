package com.fireway.backend.modules.routing.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import com.fireway.backend.modules.routing.domain.BoundingBox;
import com.fireway.backend.modules.routing.domain.Coordinate;
import com.fireway.backend.modules.routing.domain.NoGoAreaSummary;
import com.fireway.backend.modules.staticdata.application.NoGoAreaService;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import com.fireway.backend.modules.staticdata.domain.NoGoArea.GeometryType;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaticDataNoGoLookupTest {
    // 성남 중원구. 위도 37, 경도 127 이라 뒤집히면 바로 드러난다.
    private static final BoundingBox 범위 = new BoundingBox(
            new Coordinate(37.43, 127.14), new Coordinate(37.45, 127.16));

    private static NoGoArea area(long id, String extId, String status) {
        return new NoGoArea(id, extId, "금광1동", "소방차 진입곤란 지정", 1, status, "",
                GeometryType.LINE_STRING,
                List.of(new com.fireway.backend.modules.staticdata.domain.Coordinate(37.44, 127.15),
                        new com.fireway.backend.modules.staticdata.domain.Coordinate(37.441, 127.151)));
    }
    private StaticDataNoGoLookup lookup(NoGoArea... rows) {
        return new StaticDataNoGoLookup(new NoGoAreaService(() -> List.of(rows)));
    }

    /**
     * 이 테스트가 이 클래스의 존재 이유다. staticdata 는 (lat, lon) 으로 들고 있고
     * Valhalla exclude_polygons 는 [lon, lat] 를 받는다. 뒤집히면 엉뚱한 구역이 막힌다.
     */
    @Test void 좌표를_lon_lat_순서로_넘긴다() {
        List<NoGoAreaSummary> out = lookup(area(1, "ext-1", NoGoArea.OK)).forRouting(범위);
        assertThat(out).hasSize(1);
        double[] first = out.get(0).pathAsLonLat().get(0);
        assertThat(first[0]).isEqualTo(127.15);   // 경도가 앞
        assertThat(first[1]).isEqualTo(37.44);    // 위도가 뒤
    }

    @Test void ext_id_를_식별자로_쓴다() {
        assertThat(lookup(area(1, "geumgwang1-impassable-003", NoGoArea.OK)).forRouting(범위))
                .extracting(NoGoAreaSummary::polygonId)
                .containsExactly("geumgwang1-impassable-003");
    }

    /** V2_1 픽스처 2건은 ext_id 가 null 이다. 식별자가 비면 라우팅 응답에서 구간을 못 가린다. */
    @Test void ext_id_가_없으면_PK_로_식별한다() {
        assertThat(lookup(area(7, null, NoGoArea.OK)).forRouting(범위))
                .extracting(NoGoAreaSummary::polygonId)
                .containsExactly("no-go-7");
    }

    // 위성 대조에서 도로가 아닌 것으로 확인된 구간은 라우팅에 넘기지 않는다.
    @Test void 미확인_구간은_넘기지_않는다() {
        assertThat(lookup(area(1, "ext-1", NoGoArea.OK), area(2, "ext-2", NoGoArea.UNVERIFIED))
                .forRouting(범위))
                .extracting(NoGoAreaSummary::polygonId)
                .containsExactly("ext-1");
    }

    @Test void 범위_밖_구간은_넘기지_않는다() {
        BoundingBox 다른곳 = new BoundingBox(new Coordinate(37.20, 126.90), new Coordinate(37.25, 126.95));
        assertThat(lookup(area(1, "ext-1", NoGoArea.OK)).forRouting(다른곳)).isEmpty();
    }

    @Test void 사유를_그대로_전달한다() {
        assertThat(lookup(area(1, "ext-1", NoGoArea.OK)).forRouting(범위))
                .extracting(NoGoAreaSummary::reason)
                .containsExactly("소방차 진입곤란 지정");
    }
}
