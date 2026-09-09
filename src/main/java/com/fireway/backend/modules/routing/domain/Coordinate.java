package com.fireway.backend.modules.routing.domain;

import jakarta.validation.constraints.*;

public record Coordinate(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double lon) { }
