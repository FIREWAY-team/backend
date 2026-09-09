package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class GoldenTimePrioritizer {
    public List<RouteCandidate> sort(List<RouteCandidate> in, int slaSec) {
        List<RouteCandidate> copy = new ArrayList<>(in);
        for (RouteCandidate candidate : copy) candidate.setMeetsGoldenTime(candidate.etaSec() <= slaSec);
        copy.sort(Comparator.<RouteCandidate>comparingInt(c -> c.meetsGoldenTime() ? 0 : 1)
                .thenComparingInt(RouteCandidate::etaSec));
        for (int i = 0; i < copy.size(); i++) copy.get(i).setRank(i + 1);
        return copy;
    }
}
