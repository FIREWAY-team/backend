package com.fireway.backend.modules.routing.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.List;

/**
 * Wire shape for {@link RouteCandidate}. Fields are exposed in snake_case to match the shared
 * frontend contract; new fields since routing PR #11 are the 3-layer decision result:
 * {@code passable_for_vehicle}, {@code unlocked_by_cctv}, {@code has_unresolved_static_no_go}.
 */
public record RouteCandidateDto(int rank, List<double[]> coordinates, String polyline,
        @JsonProperty("eta_sec") int etaSec,
        @JsonProperty("distance_m") double distanceM,
        @JsonProperty("passable_prob") double passableProb,
        @JsonProperty("meets_golden_time") boolean meetsGoldenTime,
        @JsonProperty("passable_for_vehicle") boolean passableForVehicle,
        @JsonProperty("unlocked_by_cctv") List<String> unlockedByCctv,
        @JsonProperty("has_unresolved_static_no_go") boolean hasUnresolvedStaticNoGo,
        String explanation,
        @JsonProperty("excluded_reasons") List<ExcludedReasonDto> excludedReasons) {
    public static RouteCandidateDto from(RouteCandidate route) {
        return new RouteCandidateDto(route.rank(), route.coordinates(), route.polyline(), route.etaSec(),
                route.distanceM(), route.passableProb(), route.meetsGoldenTime(),
                route.passableForVehicle(), route.unlockedByCctv(), route.hasUnresolvedStaticNoGo(),
                route.explanation(),
                route.excludedReasons().stream().map(ExcludedReasonDto::from).toList());
    }
}
