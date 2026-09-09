package com.fireway.backend.modules.routing.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.List;

public record RouteCandidateDto(int rank, List<double[]> coordinates, String polyline,
        @JsonProperty("eta_sec") int etaSec,
        @JsonProperty("distance_m") double distanceM,
        @JsonProperty("passable_prob") double passableProb,
        @JsonProperty("meets_golden_time") boolean meetsGoldenTime,
        String explanation,
        @JsonProperty("excluded_reasons") List<ExcludedReasonDto> excludedReasons) {
    public static RouteCandidateDto from(RouteCandidate route) {
        return new RouteCandidateDto(route.rank(), route.coordinates(), route.polyline(), route.etaSec(),
                route.distanceM(), route.passableProb(), route.meetsGoldenTime(), route.explanation(),
                route.excludedReasons().stream().map(ExcludedReasonDto::from).toList());
    }
}
