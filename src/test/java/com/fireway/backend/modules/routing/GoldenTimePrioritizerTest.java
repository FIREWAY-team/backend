package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.assertThat;
import com.fireway.backend.modules.routing.application.GoldenTimePrioritizer;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoldenTimePrioritizerTest {
    private final GoldenTimePrioritizer prioritizer = new GoldenTimePrioritizer();
    @Test
    void sorts_meeting_candidates_first() {
        var result = prioritizer.sort(List.of(RoutingTestFixtures.candidate(1, 340),
                RoutingTestFixtures.candidate(2, 300), RoutingTestFixtures.candidate(3, 200)), 300);
        assertThat(result).extracting(RouteCandidate::meetsGoldenTime).containsExactly(true, true, false);
    }
    @Test
    void within_group_sorts_by_eta_asc() {
        var input = List.of(RoutingTestFixtures.candidate(1, 600), RoutingTestFixtures.candidate(2, 280),
                RoutingTestFixtures.candidate(3, 400), RoutingTestFixtures.candidate(4, 200));
        assertThat(prioritizer.sort(input, 300)).extracting(RouteCandidate::etaSec).containsExactly(200, 280, 400, 600);
        assertThat(input).extracting(RouteCandidate::etaSec).containsExactly(600, 280, 400, 200);
    }
    @Test
    void assigns_rank_1_to_n() {
        assertThat(prioritizer.sort(List.of(RoutingTestFixtures.candidate(7, 340),
                RoutingTestFixtures.candidate(9, 200), RoutingTestFixtures.candidate(4, 280)), 300))
                .extracting(RouteCandidate::rank).containsExactly(1, 2, 3);
    }
}
