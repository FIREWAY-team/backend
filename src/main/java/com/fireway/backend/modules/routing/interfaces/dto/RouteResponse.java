package com.fireway.backend.modules.routing.interfaces.dto;
import com.fireway.backend.modules.routing.domain.Route;
import java.util.List;
public record RouteResponse(String routeId, String scenarioId, double distanceM, int etaSeconds, List<WaypointResponse> waypoints) {
    public static RouteResponse from(Route r) { return new RouteResponse(r.routeId(), r.scenarioId(), r.distanceM(), r.etaSeconds(), r.waypoints().stream().map(w -> new WaypointResponse(w.lat(), w.lon(), w.instruction())).toList()); }
    public record WaypointResponse(double lat, double lon, String instruction) { }
}
