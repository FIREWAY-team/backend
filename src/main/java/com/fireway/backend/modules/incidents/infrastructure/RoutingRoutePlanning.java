package com.fireway.backend.modules.incidents.infrastructure;
import com.fireway.backend.modules.incidents.application.port.RoutePlanning;
import com.fireway.backend.modules.incidents.domain.IncidentRoute;
import com.fireway.backend.modules.routing.application.RoutePlanner;
import com.fireway.backend.modules.routing.application.RoutePlanningCommand;
import com.fireway.backend.modules.routing.domain.Coordinate;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * routing 모듈에 경로 산출을 맡기고 결과를 incidents 도메인으로 옮긴다.
 * 경계를 넘는 유일한 지점이라 변환을 여기 한 곳에 모은다.
 */
@Component
public class RoutingRoutePlanning implements RoutePlanning {
    // RouteController 와 같은 기본값을 쓴다. 신고 쪽에서 따로 조정할 이유가 아직 없다.
    private static final int CANDIDATES = 3;
    private static final double OVERLAP_THRESHOLD = 0.7;
    private static final int GOLDEN_TIME_SEC = 300;   // 5분

    private final RoutePlanner planner;
    public RoutingRoutePlanning(RoutePlanner planner) { this.planner = planner; }

    @Override
    public List<IncidentRoute> plan(String vehicleId, double fromLat, double fromLon,
                                    double toLat, double toLon) {
        var command = new RoutePlanningCommand(vehicleId,
                new Coordinate(fromLat, fromLon), new Coordinate(toLat, toLon),
                CANDIDATES, OVERLAP_THRESHOLD, GOLDEN_TIME_SEC);
        return planner.plan(command).routes().stream().map(RoutingRoutePlanning::toDomain).toList();
    }

    private static IncidentRoute toDomain(RouteCandidate c) {
        List<IncidentRoute.Blocked> blocked = c.excludedReasons().stream()
                .map(r -> new IncidentRoute.Blocked(r.polygonId(), r.reason(), r.evidenceUrl()))
                .toList();
        return new IncidentRoute(0, 0, null, c.rank(),
                (int) Math.round(c.distanceM()), c.etaSec(), c.polyline(),
                c.passableForVehicle(), c.meetsGoldenTime(), c.passableProb(),
                c.explanation() == null ? "" : c.explanation(),
                blocked, List.copyOf(c.unlockedByCctv()), null);
    }
}
