package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.fireway.backend.modules.routing.application.*;
import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.*;
import com.fireway.backend.modules.routing.infrastructure.MockNoGoLookup;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import com.fireway.backend.shared.exception.NotFoundException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Wires real planning services; database and remote routing are boundary mocks. */
@SpringBootTest(classes = RoutePlannerIntegrationTest.Config.class)
class RoutePlannerIntegrationTest {
    @Configuration(proxyBeanMethods = false)
    @Import({RoutePlanner.class, VehicleService.class, MockNoGoLookup.class,
            WeightedOverlapCalculator.class, GoldenTimePrioritizer.class, RuleBasedExplanationBuilder.class})
    static class Config { }

    @Autowired RoutePlanner planner;
    @MockitoBean ValhallaClient valhalla;
    @MockitoBean VehicleRepository vehicles;

    @BeforeEach
    void setUp() { when(vehicles.findById("pump-3.5")).thenReturn(Optional.of(RoutingTestFixtures.VEHICLE)); }

    @Test
    void plan_with_mock_valhalla_returns_normal_status() {
        when(valhalla.route(any())).thenReturn(RoutingTestFixtures.candidates());
        var result = planner.plan(RoutingTestFixtures.command());
        assertThat(result.alternativesStatus()).isEqualTo("normal");
        assertThat(result.kEffective()).isEqualTo(3);
        assertThat(result.routes()).extracting(RouteCandidate::meetsGoldenTime).containsExactly(true, true, false);
        assertThat(result.routes()).extracting(RouteCandidate::rank).containsExactly(1, 2, 3);
        assertThat(result.routes()).allSatisfy(route -> assertThat(route.explanation()).isNotBlank());
        assertThat(result.overlapMatrix()).hasDimensions(3, 3);
        assertThat(result.calcTimeMs()).isGreaterThanOrEqualTo(0);
        var request = ArgumentCaptor.forClass(RoutePlanRequest.class);
        verify(valhalla).route(request.capture());
        assertThat(request.getValue().vehicle()).isEqualTo(RoutingTestFixtures.VEHICLE);
        assertThat(request.getValue().polygons()).isEmpty();
    }

    @Test
    void plan_when_all_candidates_exceed_golden_time_returns_partial() {
        var candidates = RoutingTestFixtures.candidates();
        var slow = new ArrayList<RouteCandidate>();
        for (int i = 0; i < candidates.size(); i++) {
            var route = candidates.get(i);
            slow.add(new RouteCandidate(i + 1, route.coordinates(), route.polyline(), 400 + i * 100, route.distanceM()));
        }
        when(valhalla.route(any())).thenReturn(slow);
        var result = planner.plan(RoutingTestFixtures.command());
        assertThat(result.alternativesStatus()).isEqualTo("partial");
        assertThat(result.routes()).extracting(RouteCandidate::etaSec).containsExactly(400, 500, 600);
        assertThat(result.routes()).noneMatch(RouteCandidate::meetsGoldenTime);
    }

    @Test
    void plan_when_candidates_are_empty_returns_no_alternative() {
        when(valhalla.route(any())).thenReturn(List.of());
        var result = planner.plan(RoutingTestFixtures.command());
        assertThat(result.alternativesStatus()).isEqualTo("no_alternative");
        assertThat(result.routes()).isEmpty();
        assertThat(result.kEffective()).isZero();
        assertThat(result.overlapMatrix()).isEmpty();
    }

    @Test
    void plan_when_candidates_overlap_completely_retains_first_candidate() {
        var first = RoutingTestFixtures.candidates().get(0);
        when(valhalla.route(any())).thenReturn(List.of(first,
                new RouteCandidate(2, first.coordinates(), first.polyline(), 280, first.distanceM()),
                new RouteCandidate(3, first.coordinates(), first.polyline(), 340, first.distanceM())));
        var result = planner.plan(RoutingTestFixtures.command());
        assertThat(result.routes()).containsExactly(first);
        assertThat(result.kEffective()).isEqualTo(1);
        assertThat(result.alternativesStatus()).isEqualTo("normal");
    }

    @Test
    void plan_when_vehicle_does_not_exist_throws_not_found() {
        var command = new RoutePlanningCommand("missing", RoutingTestFixtures.FROM, RoutingTestFixtures.TO, 3, 0.65, 300);
        assertThatThrownBy(() -> planner.plan(command)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(valhalla);
    }

    @Test
    void plan_applies_requested_k_and_golden_time() {
        when(valhalla.route(any())).thenReturn(RoutingTestFixtures.candidates());
        var result = planner.plan(new RoutePlanningCommand("pump-3.5", RoutingTestFixtures.FROM,
                RoutingTestFixtures.TO, 1, 0.65, 100));
        assertThat(result.routes()).hasSize(1);
        assertThat(result.kEffective()).isEqualTo(1);
        assertThat(result.alternativesStatus()).isEqualTo("partial");
        assertThat(result.overlapMatrix()).hasDimensions(1, 1);
    }
}
