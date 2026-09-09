package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.assertThat;
import com.fireway.backend.modules.routing.application.RuleBasedExplanationBuilder;
import com.fireway.backend.modules.routing.domain.ExcludedReason;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedExplanationBuilderTest {
    @Test
    void contains_rank_and_eta() {
        var route = RoutingTestFixtures.candidate(2, 280);
        route.setMeetsGoldenTime(true);
        route.setExcludedReasons(List.of(new ExcludedReason("p1", "폭 제한", null)));
        route.setPassableProb(0.87);
        var builder = new RuleBasedExplanationBuilder();
        assertThat(builder.build(route, RoutingTestFixtures.VEHICLE, 300))
                .contains("2순위", "280초", "골든타임 5분 이내", "1건 우회", "87%", "펌프차 3.5톤", "2.1m");
        assertThat(builder.build(route, RoutingTestFixtures.VEHICLE, 400)).contains("골든타임 400초 이내");
    }
}
