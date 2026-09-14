package com.fireway.backend.modules.routing.infrastructure;

import com.fireway.backend.modules.routing.application.port.NoGoLookup;
import com.fireway.backend.modules.routing.domain.BoundingBox;
import com.fireway.backend.modules.routing.domain.NoGoAreaSummary;
import com.fireway.backend.modules.staticdata.application.NoGoAreaService;
import com.fireway.backend.modules.staticdata.domain.NoGoArea;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 진입곤란 구간을 staticdata 에서 가져온다. MockNoGoLookup 을 대신한다.
 *
 * 모듈 경계를 넘는 유일한 지점이라 변환을 여기 한 곳에 모은다 —
 * routing 도메인은 staticdata 타입을 모르고 그 반대도 마찬가지다.
 * 두 모듈에 같은 이름의 BoundingBox 가 있는 것도 일부러 그렇게 뒀다.
 */
@Component
public class StaticDataNoGoLookup implements NoGoLookup {
    private final NoGoAreaService staticData;

    public StaticDataNoGoLookup(NoGoAreaService staticData) { this.staticData = staticData; }

    @Override public List<NoGoAreaSummary> forRouting(BoundingBox box) {
        var query = new com.fireway.backend.modules.staticdata.domain.BoundingBox(
                box.southWest().lat(), box.southWest().lon(),
                box.northEast().lat(), box.northEast().lon());
        return staticData.forRouting(query).stream().map(StaticDataNoGoLookup::toSummary).toList();
    }

    private static NoGoAreaSummary toSummary(NoGoArea area) {
        // Valhalla exclude_polygons 는 [lon, lat] 순서를 받는다. staticdata 는 (lat, lon) 으로
        // 들고 있으므로 여기서 뒤집는다. 이 한 줄이 틀리면 엉뚱한 구역이 막힌다.
        List<double[]> path = area.path().stream()
                .map(c -> new double[] { c.lon(), c.lat() })
                .toList();
        // evidenceUrl 은 아직 없다. 관내도 출처(source_pdf/page)가 문서로 호스팅되면 그때 채운다.
        return new NoGoAreaSummary(identify(area), area.reason(), null, path);
    }

    /** V2_1 픽스처 2건은 ext_id 가 없다. 그쪽은 PK 로 식별한다. */
    private static String identify(NoGoArea area) {
        return area.extId() != null ? area.extId() : "no-go-" + area.id();
    }
}
