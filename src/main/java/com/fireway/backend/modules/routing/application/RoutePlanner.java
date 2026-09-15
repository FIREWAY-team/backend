package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.*;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import com.fireway.backend.shared.exception.NotFoundException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RoutePlanner {
    private final VehicleService vehicles;
    private final NoGoLookup noGoLookup;
    private final CctvReadingLookup cctvLookup;
    private final ValhallaClient valhalla;
    private final WeightedOverlapCalculator overlaps;
    private final GoldenTimePrioritizer prioritizer;
    private final ExplanationBuilder explanations;

    public RoutePlanner(VehicleService vehicles, NoGoLookup noGoLookup, CctvReadingLookup cctvLookup,
                        ValhallaClient valhalla, WeightedOverlapCalculator overlaps,
                        GoldenTimePrioritizer prioritizer, ExplanationBuilder explanations) {
        this.vehicles = vehicles;
        this.noGoLookup = noGoLookup;
        this.cctvLookup = cctvLookup;
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
        // One snapshot per plan: a PASS must unlock the edge before the router searches it.
        Map<String, Optional<CctvVerdict>> verdicts = cctvLookup.forPolygons(
                polygons.stream().map(NoGoAreaSummary::polygonId).toList());
        List<NoGoAreaSummary> blocked = polygons.stream()
                .filter(p -> verdicts.get(p.polygonId()).filter(v -> v.passes(vehicle.vehicleId())).isEmpty())
                .toList();
        List<NoGoAreaSummary> cleared = polygons.stream().filter(p -> !blocked.contains(p)).toList();
        List<RouteCandidate> candidates = valhalla.route(new RoutePlanRequest(
                command.from(), command.to(), vehicle, blocked, command.k(), cleared));
        for (RouteCandidate route : candidates) applyCctvOverlay(route, polygons, vehicle, verdicts);
        // Filter duplicates only after ranking by feasibility, otherwise a blocked shortcut can
        // suppress the usable detour that shares most of its approach road.
        List<RouteCandidate> ordered = reorderPassableFirst(prioritizer.sort(candidates, command.goldenTimeSec()));
        List<RouteCandidate> routes = overlaps.filter(ordered, command.overlapThreshold())
                .stream().limit(command.k()).toList();
        routes = reorderPassableFirst(routes);
        for (RouteCandidate route : routes) {
            route.setExplanation(explanations.build(route, vehicle, command.goldenTimeSec()));
        }
        String status = routes.isEmpty() ? "no_alternative"
                : routes.stream().anyMatch(r -> r.passableForVehicle() && r.meetsGoldenTime()) ? "normal"
                : routes.stream().anyMatch(RouteCandidate::passableForVehicle) ? "partial"
                : "blocked";
        double[][] matrix = overlaps.matrix(routes);
        boolean valhallaMocked = valhalla.getClass().getSimpleName().startsWith("Mock");
        boolean noGoMocked = noGoLookup.getClass().getSimpleName().startsWith("Mock");
        return new RoutePlanResult(routes, (System.nanoTime() - start) / 1_000_000,
                routes.size(), matrix, status, polygons.size(), valhallaMocked, noGoMocked, vehicle,
                polygons.stream().map(p -> {
                    Optional<CctvVerdict> v = verdicts.get(p.polygonId());
                    return new RoutePlanResult.CctvAssessment(p.polygonId(), p.pathAsLonLat(),
                            v.map(value -> value.verdictFor(vehicle.vehicleId())).orElse("UNKNOWN"),
                            v.map(CctvVerdict::cctvId).orElse(null), v.map(CctvVerdict::confidence).orElse(0.0));
                }).toList());
    }

    private void applyCctvOverlay(RouteCandidate route, List<NoGoAreaSummary> polygons, Vehicle vehicle,
                                  Map<String, Optional<CctvVerdict>> verdicts) {
        List<NoGoAreaSummary> touched = polygons.stream()
                .filter(polygon -> RouteGeometry.touches(route.coordinates(), polygon.pathAsLonLat()))
                .toList();
        if (touched.isEmpty()) {
            route.setExcludedReasons(List.of());
            route.setPassableForVehicle(true);
            route.setUnlockedByCctv(List.of());
            route.setHasUnresolvedStaticNoGo(false);
            route.setPassableProb(0.96);
            return;
        }
        List<ExcludedReason> stillBlocking = new ArrayList<>();
        List<String> unlockedCctvIds = new ArrayList<>();
        int failCount = 0;
        int unresolvedCount = 0;
        for (NoGoAreaSummary polygon : touched) {
            Optional<CctvVerdict> verdict = verdicts.get(polygon.polygonId());
            if (verdict.isPresent() && verdict.get().passes(vehicle.vehicleId())) {
                unlockedCctvIds.add(verdict.get().cctvId());
                continue;
            }
            stillBlocking.add(new ExcludedReason(polygon.polygonId(), polygon.reason(), polygon.evidenceUrl()));
            if (verdict.isPresent() && "FAIL".equals(verdict.get().verdictFor(vehicle.vehicleId()))) failCount++;
            else unresolvedCount++;
        }
        route.setExcludedReasons(stillBlocking);
        route.setUnlockedByCctv(List.copyOf(unlockedCctvIds));
        route.setHasUnresolvedStaticNoGo(unresolvedCount > 0);
        // Unknown and UNCERTAIN never authorize entry into a static no-go.
        route.setPassableForVehicle(failCount == 0 && unresolvedCount == 0);
        double unlockedRatio = touched.isEmpty() ? 1.0 : (double) unlockedCctvIds.size() / touched.size();
        // Mock confidence score; unresolved evidence cannot authorize a route.
        if (!route.passableForVehicle()) {
            route.setPassableProb(0.05);
        } else {
            route.setPassableProb(0.60 + unlockedRatio * 0.36);
        }
    }

    /** Sort by {@code passableForVehicle} first (true先), preserving existing rank ordering within groups. */
    private List<RouteCandidate> reorderPassableFirst(List<RouteCandidate> routes) {
        List<RouteCandidate> passable = routes.stream().filter(RouteCandidate::passableForVehicle).toList();
        List<RouteCandidate> blocked = routes.stream().filter(r -> !r.passableForVehicle()).toList();
        List<RouteCandidate> merged = new ArrayList<>(passable);
        merged.addAll(blocked);
        for (int i = 0; i < merged.size(); i++) merged.get(i).setRank(i + 1);
        return merged;
    }

}
