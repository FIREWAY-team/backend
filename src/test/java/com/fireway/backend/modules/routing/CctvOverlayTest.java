package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fireway.backend.modules.routing.application.GoldenTimePrioritizer;
import com.fireway.backend.modules.routing.application.RoutePlanner;
import com.fireway.backend.modules.routing.application.RoutePlanningCommand;
import com.fireway.backend.modules.routing.application.RuleBasedExplanationBuilder;
import com.fireway.backend.modules.routing.application.WeightedOverlapCalculator;
import com.fireway.backend.modules.routing.application.port.CctvReadingLookup;
import com.fireway.backend.modules.routing.application.port.NoGoLookup;
import com.fireway.backend.modules.routing.application.port.ValhallaClient;
import com.fireway.backend.modules.routing.domain.CctvVerdict;
import com.fireway.backend.modules.routing.domain.Coordinate;
import com.fireway.backend.modules.routing.domain.NoGoAreaSummary;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Focused unit tests for the 3-layer decision (static no-go × CCTV verdict × vehicle) that
 * {@link RoutePlanner#plan} applies. Uses hand-built polygons that we know intersect the mock
 * route path, and a controlled {@link CctvReadingLookup} stub that answers per polygon id.
 * <p>
 * Test structure — one route, one polygon on its path, we swap only the CCTV verdict between
 * cases and observe how the planner classifies the route.
 */
class CctvOverlayTest {

    private static final Coordinate FROM = new Coordinate(37.44, 127.14);
    private static final Coordinate TO = new Coordinate(37.45, 127.16);
    private static final RoutePlanningCommand COMMAND =
            new RoutePlanningCommand("pump-3.5", FROM, TO, 3, 0.65, 300);

    private RoutePlanner build(CctvReadingLookup cctv, List<NoGoAreaSummary> polygons) {
        var vehicles = mock(VehicleRepository.class);
        when(vehicles.findById("pump-3.5")).thenReturn(Optional.of(RoutingTestFixtures.VEHICLE));
        // 폴리곤은 경로 중점 근처(라우트 시작·중간·끝 세 점의 평균 근처)에 배치해서 반드시 touch 되게 한다.
        NoGoLookup noGoLookup = box -> polygons;
        ValhallaClient valhalla = mock(ValhallaClient.class);
        when(valhalla.route(any())).thenReturn(RoutingTestFixtures.candidates());
        return new RoutePlanner(new VehicleService(vehicles), noGoLookup, cctv, valhalla,
                new WeightedOverlapCalculator(), new GoldenTimePrioritizer(),
                new RuleBasedExplanationBuilder());
    }

    /**
     * Polygon centred on the shared start point of every fixture candidate so {@code isNearRoute}
     * hits all three routes (mock generates candidates with a jittered midpoint but the same
     * start/end — polygon on start guarantees a touch on every route).
     */
    private static NoGoAreaSummary polygonOnRoute(String id) {
        return new NoGoAreaSummary(id, "골목 좁음", null, List.of(
                new double[]{FROM.lon(), FROM.lat()},
                new double[]{FROM.lon() + 0.00005, FROM.lat() + 0.00005}));
    }

    @Test
    void cctv_pass_verdict_unlocks_the_no_go_for_that_vehicle() {
        var polygon = polygonOnRoute("poly-pass");
        CctvReadingLookup cctv = id -> Optional.of(new CctvVerdict("cctv-42", 2.6,
                Map.of("pump-3.5", "PASS", "pump-8", "FAIL"), 0.9));

        var result = build(cctv, List.of(polygon)).plan(COMMAND);

        assertThat(result.routes()).allSatisfy(route -> {
            assertThat(route.passableForVehicle()).isTrue();
            assertThat(route.unlockedByCctv()).contains("cctv-42");
            assertThat(route.excludedReasons()).isEmpty();
            assertThat(route.hasUnresolvedStaticNoGo()).isFalse();
        });
        assertThat(result.alternativesStatus()).isEqualTo("normal");
    }

    @Test
    void cctv_fail_verdict_hard_blocks_the_route_for_that_vehicle() {
        var polygon = polygonOnRoute("poly-fail");
        CctvReadingLookup cctv = id -> Optional.of(new CctvVerdict("cctv-77", 1.9,
                Map.of("pump-3.5", "FAIL", "pump-8", "FAIL"), 0.88));

        var result = build(cctv, List.of(polygon)).plan(COMMAND);

        assertThat(result.routes()).allSatisfy(route -> {
            assertThat(route.passableForVehicle()).isFalse();
            assertThat(route.excludedReasons()).extracting(ExcludedReasonId::of).contains("poly-fail");
            assertThat(route.passableProb()).isLessThan(0.2);
        });
        assertThat(result.alternativesStatus()).isEqualTo("blocked");
    }

    @Test
    void missing_cctv_verdict_marks_the_no_go_unresolved_but_does_not_hard_block() {
        var polygon = polygonOnRoute("poly-silent");
        CctvReadingLookup cctv = id -> Optional.empty();

        var result = build(cctv, List.of(polygon)).plan(COMMAND);

        assertThat(result.routes()).allSatisfy(route -> {
            assertThat(route.hasUnresolvedStaticNoGo()).isTrue();
            assertThat(route.passableForVehicle()).isTrue();
            assertThat(route.excludedReasons()).extracting(ExcludedReasonId::of).contains("poly-silent");
            assertThat(route.unlockedByCctv()).isEmpty();
        });
        assertThat(result.alternativesStatus()).isEqualTo("normal");
    }

    @Test
    void route_untouched_by_any_polygon_needs_no_cctv_and_is_clean() {
        // 경로에서 멀리 떨어진 폴리곤 — isNearRoute 가 false 리턴, CCTV 조회 자체가 발생 안 함.
        var farAway = new NoGoAreaSummary("poly-far", "다른 동", null, List.of(
                new double[]{128.5, 37.9},
                new double[]{128.6, 38.0}));
        CctvReadingLookup cctv = id -> {
            throw new AssertionError("lookup 이 호출되면 안 된다");
        };

        var result = build(cctv, List.of(farAway)).plan(COMMAND);

        assertThat(result.routes()).allSatisfy(route -> {
            assertThat(route.passableForVehicle()).isTrue();
            assertThat(route.excludedReasons()).isEmpty();
            assertThat(route.unlockedByCctv()).isEmpty();
            assertThat(route.hasUnresolvedStaticNoGo()).isFalse();
            assertThat(route.passableProb()).isEqualTo(0.96);
        });
    }

    /** Tiny helper for extracting the id from {@link com.fireway.backend.modules.routing.domain.ExcludedReason} in assertions. */
    private static class ExcludedReasonId {
        static String of(com.fireway.backend.modules.routing.domain.ExcludedReason r) { return r.polygonId(); }
    }
}
