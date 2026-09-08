package team.fireway.goldenlane.modules.routing.interfaces.dto;
import java.util.List;
public record RouteResponse(String scenarioId,String vehicleId,List<?> routes,Object explanation) {}

