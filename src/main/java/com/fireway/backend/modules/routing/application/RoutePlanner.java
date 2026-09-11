package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.*;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import com.fireway.backend.shared.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoutePlanner {
    private final VehicleService vehicles;
    private final NoGoLookup noGoLookup;
    private final ValhallaClient valhalla;
    private final WeightedOverlapCalculator overlaps;
    private final GoldenTimePrioritizer prioritizer;
    private final ExplanationBuilder explanations;

    public RoutePlanner(VehicleService vehicles, NoGoLookup noGoLookup, ValhallaClient valhalla,
                        WeightedOverlapCalculator overlaps, GoldenTimePrioritizer prioritizer,
                        ExplanationBuilder explanations) {
        this.vehicles = vehicles;
        this.noGoLookup = noGoLookup;
        this.valhalla = valhalla;
        this.overlaps = overlaps;
        this.prioritizer = prioritizer;
        this.explanations = explanations;
    }

    public RoutePlanResult plan(RoutePlanningCommand command) {
        long start = System.nanoTime();
        Vehicle vehicle = vehicles.findById(command.vehicleId())
                .orElseThrow(() -> new NotFoundException("차량을 찾을 수 없습니다: " + command.vehicleId()));
        List<NoGoAreaSummary> polygons = noGoLookup.forRouting(BoundingBox.around(command.from(), command.to(), 500));
        List<RouteCandidate> candidates = valhalla.route(new RoutePlanRequest(
                command.from(), command.to(), vehicle, polygons, command.k()));
        List<RouteCandidate> filtered = overlaps.filter(candidates, command.overlapThreshold());
        List<RouteCandidate> routes = prioritizer.sort(filtered, command.goldenTimeSec())
                .stream().limit(command.k()).toList();
        for (RouteCandidate route : routes) {
            // '이 경로가 우회한 근거'로 표시되는 excludedReasons — Valhalla 는 우회 이유를 응답에
            // 노출하지 않으므로, 경로 좌표열에서 일정 반경(120m) 안에 있는 활성 no-go 폴리곤만
            // 근접 우회 근거로 붙인다. 실 tile 도입 후 링크 매칭으로 정확화 예정 (TODO(routing/v2)).
            List<ExcludedReason> nearby = polygons.stream()
                    .filter(polygon -> isNearRoute(route, polygon, 120.0))
                    .map(polygon -> new ExcludedReason(polygon.polygonId(), polygon.reason(), polygon.evidenceUrl()))
                    .toList();
            route.setExcludedReasons(nearby);
            route.setExplanation(explanations.build(route, vehicle, command.goldenTimeSec()));
        }
        String status = routes.isEmpty() ? "no_alternative"
                : routes.stream().anyMatch(RouteCandidate::meetsGoldenTime) ? "normal" : "partial";
        double[][] matrix = overlaps.matrix(routes);
        boolean valhallaMocked = valhalla.getClass().getSimpleName().startsWith("Mock");
        boolean noGoMocked = noGoLookup.getClass().getSimpleName().startsWith("Mock");
        return new RoutePlanResult(routes, (System.nanoTime() - start) / 1_000_000,
                routes.size(), matrix, status, polygons.size(), valhallaMocked, noGoMocked, vehicle);
    }

    private boolean isNearRoute(RouteCandidate route, NoGoAreaSummary polygon, double meters) {
        // 폴리곤 대표점(좌표 평균)과 경로 각 점 사이 최소 거리로 근사 판단.
        List<double[]> path = polygon.pathAsLonLat();
        if (path == null || path.isEmpty()) return false;
        double centroidLon = path.stream().mapToDouble(p -> p[0]).average().orElse(0);
        double centroidLat = path.stream().mapToDouble(p -> p[1]).average().orElse(0);
        for (double[] point : route.coordinates()) {
            double dLon = point[0] - centroidLon, dLat = point[1] - centroidLat;
            double distance = 111_320
                    * Math.hypot(dLon * Math.cos(Math.toRadians(point[1])), dLat);
            if (distance <= meters) return true;
        }
        return false;
    }
}
