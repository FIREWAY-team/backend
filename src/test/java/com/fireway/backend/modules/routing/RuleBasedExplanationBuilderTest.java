package com.fireway.backend.modules.routing;

import static org.assertj.core.api.Assertions.assertThat;
import com.fireway.backend.modules.routing.application.RuleBasedExplanationBuilder;
import com.fireway.backend.modules.routing.domain.ExcludedReason;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedExplanationBuilderTest {
    /**
     * Explanation format was widened in the CCTV-overlay PR to describe the 3-layer decision (static
     * no-go × CCTV verdict × vehicle). The three legacy assertions from PR #11 (rank, ETA, golden
     * time, passableProb, vehicle) still hold; on top of those, this test now also asserts the CCTV
     * summary is present.
     */
    @Test
    void contains_rank_and_eta() {
        var route = RoutingTestFixtures.candidate(2, 280);
        route.setMeetsGoldenTime(true);
        route.setExcludedReasons(List.of(new ExcludedReason("p1", "폭 제한", null)));
        route.setPassableProb(0.87);
        var builder = new RuleBasedExplanationBuilder();
        // 원본 5 개 (rank·eta·golden·prob·vehicle) + 신규 CCTV 서머리에 나오는 "정적 진입곤란 1건".
        assertThat(builder.build(route, RoutingTestFixtures.VEHICLE, 300))
                .contains("2순위", "280초", "골든타임 5분 이내", "87%", "소형펌프차", "2.3m", "정적 진입곤란 1건");
        assertThat(builder.build(route, RoutingTestFixtures.VEHICLE, 400)).contains("골든타임 400초 이내");
    }
}
