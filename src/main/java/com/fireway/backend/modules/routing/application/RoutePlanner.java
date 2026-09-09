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
            route.setExcludedReasons(polygons.stream()
                    .map(p -> new ExcludedReason(p.polygonId(), p.reason(), p.evidenceUrl())).toList());
            route.setExplanation(explanations.build(route, vehicle, command.goldenTimeSec()));
        }
        String status = routes.isEmpty() ? "no_alternative"
                : routes.stream().anyMatch(RouteCandidate::meetsGoldenTime) ? "normal" : "partial";
        double[][] matrix = overlaps.matrix(routes);
        return new RoutePlanResult(routes, (System.nanoTime() - start) / 1_000_000,
                routes.size(), matrix, status);
    }
}
