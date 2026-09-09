package com.fireway.backend.modules.routing.application;

import com.fireway.backend.modules.routing.domain.Coordinate;

public record RoutePlanningCommand(String vehicleId, Coordinate from, Coordinate to,
                                   int k, double overlapThreshold, int goldenTimeSec) { }
