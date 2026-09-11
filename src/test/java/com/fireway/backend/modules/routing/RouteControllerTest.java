package com.fireway.backend.modules.routing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import com.fireway.backend.modules.routing.application.*;
import com.fireway.backend.modules.routing.interfaces.RouteController;
import com.fireway.backend.modules.routing.domain.ExcludedReason;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RouteController.class)
class RouteControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean RoutePlanner planner;

    @Test
    void route_returns_200_with_3_candidates() throws Exception {
        var candidates = new GoldenTimePrioritizer().sort(RoutingTestFixtures.candidates(), 300);
        candidates.get(0).setExcludedReasons(List.of(new ExcludedReason("polygon-1", "폭 제한", "https://example.com/evidence")));
        when(planner.plan(any())).thenReturn(new RoutePlanResult(candidates, 12, 3,
                new WeightedOverlapCalculator().matrix(candidates), "normal",
                0, true, true, RoutingTestFixtures.VEHICLE));
        mvc.perform(post("/api/route").contentType("application/json").content("""
                {"vehicle_id":"pump-3.5","from":{"lat":37.44,"lon":127.14},"to":{"lat":37.45,"lon":127.16}}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.routes", hasSize(3)))
                .andExpect(jsonPath("$.calc_time_ms").value(12)).andExpect(jsonPath("$.k_effective").value(3))
                .andExpect(jsonPath("$.overlap_matrix[0][0]").value(1.0))
                .andExpect(jsonPath("$.alternatives_status").value("normal"))
                .andExpect(jsonPath("$.routes[0].eta_sec").value(200))
                .andExpect(jsonPath("$.routes[0].distance_m").isNumber())
                .andExpect(jsonPath("$.routes[0].passable_prob").value(1.0))
                .andExpect(jsonPath("$.routes[0].meets_golden_time").value(true))
                .andExpect(jsonPath("$.routes[2].meets_golden_time").value(false))
                .andExpect(jsonPath("$.routes[0].coordinates[0][0]").value(127.14))
                .andExpect(jsonPath("$.routes[0].coordinates[0][1]").value(37.44))
                .andExpect(jsonPath("$.routes[0].polyline").isString())
                .andExpect(jsonPath("$.routes[0].excluded_reasons[0].polygon_id").value("polygon-1"))
                .andExpect(jsonPath("$.routes[0].excluded_reasons[0].evidence_url").value("https://example.com/evidence"))
                .andExpect(jsonPath("$.calcTimeMs").doesNotExist())
                // 신규 필드 검증: warnings + vehicle_used + no_go_considered.
                .andExpect(jsonPath("$.no_go_considered").value(0))
                .andExpect(jsonPath("$.vehicle_used.id").value("pump-3.5"))
                .andExpect(jsonPath("$.vehicle_used.width_m").value(2.3))
                .andExpect(jsonPath("$.vehicle_used.turning_radius_m").value(6.5))
                .andExpect(jsonPath("$.warnings", hasSize(2)))
                .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.startsWith("valhalla_mock")))
                .andExpect(jsonPath("$.warnings[1]").value(org.hamcrest.Matchers.startsWith("no_go_mock")));
        verify(planner).plan(RoutingTestFixtures.command());
    }

    @Test
    void route_returns_400_when_vehicle_id_missing() throws Exception {
        mvc.perform(post("/api/route").contentType("application/json").content("""
                {"from":{"lat":37.44,"lon":127.14},"to":{"lat":37.45,"lon":127.16}}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(planner);
    }

    @Test
    void route_returns_400_when_from_missing() throws Exception {
        mvc.perform(post("/api/route").contentType("application/json").content("""
                {"vehicle_id":"pump-3.5","to":{"lat":37.45,"lon":127.16}}
                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(planner);
    }
}
