package com.fireway.backend.modules.routing;

import com.fireway.backend.modules.routing.application.*;
import com.fireway.backend.modules.routing.application.port.RoutePlanRequest;
import com.fireway.backend.modules.routing.domain.*;
import com.fireway.backend.modules.routing.infrastructure.MockValhallaClient;
import com.fireway.backend.modules.vehicles.domain.Vehicle;
import java.util.List;

final class RoutingTestFixtures {
    static final Coordinate FROM = new Coordinate(37.44, 127.14);
    static final Coordinate TO = new Coordinate(37.45, 127.16);
    static final Vehicle VEHICLE = new Vehicle("pump-3.5", "소형펌프차", 2.3, 3.0, 7.0, 3.5, 6.5);
    static RoutePlanningCommand command() { return new RoutePlanningCommand(VEHICLE.vehicleId(), FROM, TO, 3, 0.65, 300); }
    static List<RouteCandidate> candidates() {
        return new MockValhallaClient().route(new RoutePlanRequest(FROM, TO, VEHICLE, List.of(), 3));
    }
    static RouteCandidate candidate(int rank, int eta, double[]... points) {
        return new RouteCandidate(rank, List.of(points), "", eta, 1000);
    }
}
