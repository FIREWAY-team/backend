package com.fireway.backend.modules.routing.application.port;

import com.fireway.backend.modules.routing.domain.RouteCandidate;
import java.util.List;

public interface ValhallaClient {
    List<RouteCandidate> route(RoutePlanRequest request);
}
