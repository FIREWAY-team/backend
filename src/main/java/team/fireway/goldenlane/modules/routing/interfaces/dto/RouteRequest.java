package team.fireway.goldenlane.modules.routing.interfaces.dto;
import jakarta.validation.constraints.NotBlank;
public record RouteRequest(@NotBlank String scenarioId,@NotBlank String vehicleId) {}

