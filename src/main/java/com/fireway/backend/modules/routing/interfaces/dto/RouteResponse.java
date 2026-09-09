package com.fireway.backend.modules.routing.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fireway.backend.modules.routing.application.RoutePlanResult;
import java.util.List;

public record RouteResponse(List<RouteCandidateDto> routes,
        @JsonProperty("calc_time_ms") long calcTimeMs,
        @JsonProperty("k_effective") int kEffective,
        @JsonProperty("overlap_matrix") double[][] overlapMatrix,
        @JsonProperty("alternatives_status") String alternativesStatus) {
    public static RouteResponse from(RoutePlanResult result) {
        return new RouteResponse(result.routes().stream().map(RouteCandidateDto::from).toList(),
                result.calcTimeMs(), result.kEffective(), result.overlapMatrix(), result.alternativesStatus());
    }
}
