package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fireway.backend.modules.routing.application.*;
import com.fireway.backend.modules.routing.application.port.*;
import com.fireway.backend.modules.routing.domain.*;
import com.fireway.backend.modules.vehicles.application.VehicleService;
import com.fireway.backend.modules.vehicles.application.port.VehicleRepository;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.*;
import org.junit.jupiter.api.Test;

class CctvRoutingSearchTest {
    @Test
    void each_vehicle_unlocks_only_its_pass_edges_before_search() {
        var road = new NoGoAreaSummary("alley", "좁은 골목", null,
                List.of(new double[]{127.14, 37.44}, new double[]{127.141, 37.441}));
        for (String vehicleId : List.of("pump-3.5", "pump-8", "pump-15")) {
            var repository = mock(VehicleRepository.class);
            when(repository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle(vehicleId, vehicleId, 2.5, 3, 7, 8, 6)));
            List<RoutePlanRequest> requests = new ArrayList<>();
            int[] reads = {0};
            CctvReadingLookup cctv = id -> {
                reads[0]++;
                return Optional.of(new CctvVerdict("camera", 2.7,
                        Map.of("pump-3.5", "PASS", "pump-8", "UNCERTAIN", "pump-15", "FAIL"), .9));
            };
            ValhallaClient router = request -> {
                assertThat(reads[0]).isEqualTo(1);
                requests.add(request);
                return RoutingTestFixtures.candidates();
            };
            var planner = new RoutePlanner(new VehicleService(repository), box -> List.of(road), cctv, router,
                    new WeightedOverlapCalculator(), new GoldenTimePrioritizer(), new RuleBasedExplanationBuilder());
            var result = planner.plan(new RoutePlanningCommand(vehicleId, RoutingTestFixtures.FROM, RoutingTestFixtures.TO, 3, .65, 300));
            boolean pass = vehicleId.equals("pump-3.5");
            assertThat(requests.get(0).polygons()).hasSize(pass ? 0 : 1);
            assertThat(requests.get(0).cleared()).hasSize(pass ? 1 : 0);
            assertThat(result.routes()).allSatisfy(r -> assertThat(r.passableForVehicle()).isEqualTo(pass));
            assertThat(reads[0]).isEqualTo(1);
        }
    }

    @Test
    void blocked_shortcut_cannot_remove_overlapping_passable_detour() {
        var repository = mock(VehicleRepository.class);
        when(repository.findById("pump-3.5")).thenReturn(Optional.of(RoutingTestFixtures.VEHICLE));
        var blocked = new NoGoAreaSummary("blocked", "불법주차", null,
                List.of(new double[]{127.151, 37.44}, new double[]{127.1515, 37.44}));
        var shortRoute = RoutingTestFixtures.candidate(1, 100,
                new double[]{127.14, 37.44}, new double[]{127.15, 37.44}, new double[]{127.152, 37.44});
        var detour = RoutingTestFixtures.candidate(2, 120,
                new double[]{127.14, 37.44}, new double[]{127.15, 37.44}, new double[]{127.15, 37.441}, new double[]{127.152, 37.441});
        var planner = new RoutePlanner(new VehicleService(repository), box -> List.of(blocked), id -> Optional.empty(),
                request -> List.of(shortRoute, detour), new WeightedOverlapCalculator(), new GoldenTimePrioritizer(), new RuleBasedExplanationBuilder());
        var result = planner.plan(new RoutePlanningCommand("pump-3.5", RoutingTestFixtures.FROM, RoutingTestFixtures.TO, 1, .5, 300));
        assertThat(result.routes()).hasSize(1);
        assertThat(result.routes().get(0)).isSameAs(detour);
        assertThat(detour.rank()).isEqualTo(1);
        assertThat(detour.passableForVehicle()).isTrue();
    }

    @Test
    void road_intersections_check_segments_without_blocking_parallel_alley() {
        List<double[]> route = List.of(new double[]{127.14, 37.44}, new double[]{127.16, 37.44});
        assertThat(RouteGeometry.touches(route, List.of(new double[]{127.15, 37.439}, new double[]{127.15, 37.441}))).isTrue();
        assertThat(RouteGeometry.touches(route, List.of(new double[]{127.14, 37.4403}, new double[]{127.16, 37.4403}))).isFalse();
        var ring = RouteGeometry.exclusionRing(List.of(new double[]{127.14, 37.44}, new double[]{127.15, 37.44}));
        assertThat(ring).hasSizeGreaterThan(3);
        assertThat(ring.get(0)).containsExactly(ring.get(ring.size() - 1));
    }
}
