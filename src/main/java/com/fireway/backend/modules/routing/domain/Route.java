package com.fireway.backend.modules.routing.domain;
import java.util.List;
public record Route(String routeId, String scenarioId, double distanceM, int etaSeconds, List<Waypoint> waypoints) { }
