package com.fireway.backend.modules.routing.application;
import com.fireway.backend.modules.routing.application.port.RouteRepository;
import com.fireway.backend.modules.routing.domain.*;
import java.util.List;
import org.springframework.stereotype.Service;
/** @deprecated Retained legacy mock; use RoutePlanner for routing. */
@Deprecated
@Service public class RouteService {
    public RouteService(RouteRepository ignored) { }
    public Route mock(RouteRequestValues v) { return new Route("route-mock-01", v.scenarioId(), 1840.0, 420, List.of(new Waypoint(v.lat(), v.lon(), "출발지"), new Waypoint(37.4381, 127.1422, "소방 진입"))); }
    public record RouteRequestValues(String scenarioId, double lat, double lon, String vehicleId) { }
}
