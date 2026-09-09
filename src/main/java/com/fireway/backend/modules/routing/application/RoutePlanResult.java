package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.List;

public record RoutePlanResult(List<RouteCandidate> routes, long calcTimeMs, int kEffective,
                              double[][] overlapMatrix, String alternativesStatus) { }
