package com.fireway.backend.modules.routing.interfaces.dto;
import jakarta.validation.constraints.NotBlank;
public record RouteRequest(@NotBlank String scenarioId, double lat, double lon, String vehicleId) { }
