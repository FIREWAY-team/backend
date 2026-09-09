package com.fireway.backend.modules.routing.application.port;

import com.fireway.backend.modules.routing.domain.*;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.List;

public record RoutePlanRequest(Coordinate from, Coordinate to, Vehicle vehicle,
                               List<NoGoAreaSummary> polygons, int k) { }
