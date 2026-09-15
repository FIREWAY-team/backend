package com.fireway.backend.modules.incidents.application.port;
import com.fireway.backend.modules.incidents.domain.IncidentRoute;
import java.util.List;
/**
 * 경로 산출. 구현은 routing 모듈을 부르지만 incidents 는 그걸 모른다.
 * 모듈 경계를 넘는 지점이라 변환은 어댑터 한 곳에 모은다(StaticDataNoGoLookup 과 같은 방식).
 */
public interface RoutePlanning {
    /** 출발지에서 신고 지점까지. rank 오름차순, id 와 incidentId 는 저장 시점에 채워진다. */
    List<IncidentRoute> plan(String vehicleId, double fromLat, double fromLon,
                             double toLat, double toLon);
}
