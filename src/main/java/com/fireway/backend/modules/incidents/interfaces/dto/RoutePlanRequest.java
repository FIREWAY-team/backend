package com.fireway.backend.modules.incidents.interfaces.dto;
import jakarta.validation.constraints.*;
/** 출동 경로 산출 요청. 도착지는 신고 좌표라 받지 않는다. */
public record RoutePlanRequest(
        @NotBlank String vehicleId,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double fromLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double fromLon) { }
