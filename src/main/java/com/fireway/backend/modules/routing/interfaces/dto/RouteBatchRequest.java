package com.fireway.backend.modules.routing.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fireway.backend.modules.routing.application.RoutePlanningCommand;
import com.fireway.backend.modules.routing.domain.Coordinate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** 같은 출발·도착을 차량 여러 대로. 나머지 필드와 기본값은 {@link RouteRequest} 와 같다. */
public record RouteBatchRequest(
        @JsonProperty("vehicle_ids") @NotEmpty @Size(max = 5) List<@NotBlank String> vehicleIds,
        @NotNull @Valid Coordinate from,
        @NotNull @Valid Coordinate to,
        @Min(1) @Max(3) Integer k,
        @JsonProperty("overlap_threshold") @DecimalMin("0") @DecimalMax("1") Double overlapThreshold,
        @JsonProperty("golden_time_sec") @Min(1) Integer goldenTimeSec) {
    public RoutePlanningCommand toCommand(String vehicleId) {
        return new RouteRequest(vehicleId, from, to, k, overlapThreshold, goldenTimeSec).toCommand();
    }
}
