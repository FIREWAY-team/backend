package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.domain.RouteCandidate;
import com.fireway.backend.modules.vehicles.domain.Vehicle;

public interface ExplanationBuilder {
    String build(RouteCandidate candidate, Vehicle vehicle, int goldenTimeSec);
}
