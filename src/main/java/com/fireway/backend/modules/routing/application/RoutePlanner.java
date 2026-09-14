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
    /**
     * Distance in meters within which a route point is considered to touch a no-go polygon. Matches the
     * threshold used by {@link #isNearRoute(RouteCandidate, NoGoAreaSummary, double)} elsewhere in this
     * class. Kept as a constant so tests and explanations share a single source of truth.
     */
    private static final double NO_GO_TOUCH_METERS = 120.0;

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
        List<RouteCandidate> candidates = valhalla.route(new RoutePlanRequest(
                command.from(), command.to(), vehicle, polygons, command.k()));
        List<RouteCandidate> filtered = overlaps.filter(candidates, command.overlapThreshold());
        List<RouteCandidate> routes = prioritizer.sort(filtered, command.goldenTimeSec())
                .stream().limit(command.k()).toList();
        for (RouteCandidate route : routes) {
            applyCctvOverlay(route, polygons, vehicle);
            route.setExplanation(explanations.build(route, vehicle, command.goldenTimeSec()));
        }
        // 통과 가능 후보를 상위로 다시 정렬 (골든타임 정렬은 앞선 prioritizer 가 이미 매겼고, 여기서는
        // "차량으로 실제 갈 수 있는 경로"를 앞으로 끌어올리는 마지막 층).
        routes = reorderPassableFirst(routes);
        String status = routes.isEmpty() ? "no_alternative"
                : routes.stream().anyMatch(r -> r.passableForVehicle() && r.meetsGoldenTime()) ? "normal"
                : routes.stream().anyMatch(RouteCandidate::passableForVehicle) ? "partial"
                : "blocked";
        double[][] matrix = overlaps.matrix(routes);
        boolean valhallaMocked = valhalla.getClass().getSimpleName().startsWith("Mock");
        boolean noGoMocked = noGoLookup.getClass().getSimpleName().startsWith("Mock");
        return new RoutePlanResult(routes, (System.nanoTime() - start) / 1_000_000,
                routes.size(), matrix, status, polygons.size(), valhallaMocked, noGoMocked, vehicle);
    }

    /**
     * Apply the 3-layer decision (static no-go × CCTV verdict × vehicle) to a single route, mutating
     * the passed candidate in place.
     * <p>
     * For every static no-go this route touches within {@link #NO_GO_TOUCH_METERS} meters, ask the
     * CCTV lookup for a per-vehicle verdict:
     * <ul>
     *   <li>Verdict PASSes the vehicle → the no-go is <b>unlocked</b>. The polygon is dropped from
     *       {@code excludedReasons} and the cctv id is recorded in {@code unlockedByCctv}.</li>
     *   <li>Verdict FAILs the vehicle → the polygon stays in {@code excludedReasons} and the route
     *       is marked <b>impassable</b> for this vehicle (a hard block, since we have real evidence
     *       the vehicle does not fit).</li>
     *   <li>No CCTV verdict at all → we cannot rule out the block. The polygon stays in
     *       {@code excludedReasons}, but the route is only flagged as {@code hasUnresolvedStaticNoGo};
     *       whether it is still considered passable falls back to the wider planner heuristics rather
     *       than being hard-refused, since the static baseline may itself be stale.</li>
     * </ul>
     *
     * {@code passableProb} moves with the ratio of unlocked touches — a route that clears every
     * touched no-go by CCTV PASS lands near {@code 0.96}; a route with pending unresolved touches
     * lands lower, proportional to the ratio.
     */
    private void applyCctvOverlay(RouteCandidate route, List<NoGoAreaSummary> polygons, Vehicle vehicle) {
        List<NoGoAreaSummary> touched = polygons.stream()
                .filter(polygon -> isNearRoute(route, polygon, NO_GO_TOUCH_METERS))
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
            Optional<CctvVerdict> verdict = cctvLookup.forPolygon(polygon.polygonId());
            if (verdict.isPresent() && verdict.get().passes(vehicle.vehicleId())) {
                unlockedCctvIds.add(verdict.get().cctvId());
                continue;
            }
            stillBlocking.add(new ExcludedReason(polygon.polygonId(), polygon.reason(), polygon.evidenceUrl()));
            if (verdict.isPresent()) failCount++;
            else unresolvedCount++;
        }
        route.setExcludedReasons(stillBlocking);
        route.setUnlockedByCctv(List.copyOf(unlockedCctvIds));
        route.setHasUnresolvedStaticNoGo(unresolvedCount > 0);
        // 하드 차단: CCTV 가 "이 차량 FAIL" 이라고 실 판정한 폴리곤이 하나라도 남으면 진입 불가.
        route.setPassableForVehicle(failCount == 0);
        double unlockedRatio = touched.isEmpty() ? 1.0 : (double) unlockedCctvIds.size() / touched.size();
        // 통과 가능한 경로: CCTV 로 클리어된 비율이 높을수록 0.96 근처. 미해결(unresolved) 남으면 0.7~0.85.
        // 진입 불가 경로: 어쨌든 못 감. 확률 0.05 로 눌러서 상황실이 실수로 선택하지 않게.
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
