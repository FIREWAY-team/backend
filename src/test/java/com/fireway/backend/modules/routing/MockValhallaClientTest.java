package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.*;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import com.fireway.backend.modules.routing.infrastructure.PolylineCodec;
import org.junit.jupiter.api.Test;

class MockValhallaClientTest {
    @Test
    void mock_returns_3_candidates_with_expected_etas() {
        var routes = RoutingTestFixtures.candidates();
        assertThat(routes).extracting(RouteCandidate::etaSec).containsExactly(200, 280, 340);
        assertThat(routes.get(1).distanceM()).isCloseTo(routes.get(0).distanceM() * 1.15, within(0.0001));
        assertThat(routes.get(2).distanceM()).isCloseTo(routes.get(0).distanceM() * 1.3, within(0.0001));
        assertThat(routes.get(0).coordinates().get(1)).isNotEqualTo(routes.get(1).coordinates().get(1));
        for (var route : routes) {
            assertThat(route.coordinates().get(0)).containsExactly(127.14, 37.44);
            assertThat(route.coordinates().get(2)).containsExactly(127.16, 37.45);
            var decoded = PolylineCodec.decode(route.polyline());
            assertThat(decoded).hasSize(3);
            for (int i = 0; i < 3; i++) {
                assertThat(decoded.get(i)[0]).isCloseTo(route.coordinates().get(i)[0], within(0.000001));
                assertThat(decoded.get(i)[1]).isCloseTo(route.coordinates().get(i)[1], within(0.000001));
            }
        }
    }
}
