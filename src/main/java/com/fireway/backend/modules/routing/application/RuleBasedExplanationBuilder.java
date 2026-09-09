package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.domain.RouteCandidate;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedExplanationBuilder implements ExplanationBuilder {
    @Override
    public String build(RouteCandidate candidate, Vehicle vehicle, int goldenTimeSec) {
        String within = goldenTimeSec == 300 ? "골든타임 5분 이내" : "골든타임 " + goldenTimeSec + "초 이내";
        return String.format(Locale.ROOT,
                "%d순위 후보. 예상 소요 %d초 (%s). 정적 진입곤란 %d건 우회. 통과확률 %.0f%%. 차량: %s (폭 %sm).",
                candidate.rank(), candidate.etaSec(), candidate.meetsGoldenTime() ? within : "골든타임 초과",
                candidate.excludedReasons().size(), candidate.passableProb() * 100, vehicle.name(), vehicle.widthM());
    }
}
