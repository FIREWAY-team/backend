package com.fireway.backend.modules.routing.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fireway.backend.modules.routing.application.RoutePlanningCommand;
import com.fireway.backend.modules.routing.domain.Coordinate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RouteRequest(
        @JsonProperty("vehicle_id") @NotBlank String vehicleId,
        @NotNull @Valid Coordinate from,
        @NotNull @Valid Coordinate to,
        @Min(1) @Max(3) Integer k,
        @JsonProperty("overlap_threshold") @DecimalMin("0") @DecimalMax("1") Double overlapThreshold,
        @JsonProperty("golden_time_sec") @Min(1) Integer goldenTimeSec) {
    public RoutePlanningCommand toCommand() {
        return new RoutePlanningCommand(vehicleId, from, to, k == null ? 3 : k,
                overlapThreshold == null ? 0.65 : overlapThreshold, goldenTimeSec == null ? 300 : goldenTimeSec);
    }
}
