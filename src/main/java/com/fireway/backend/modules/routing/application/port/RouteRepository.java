package com.fireway.backend.modules.routing.application.port;
import com.fireway.backend.modules.routing.domain.Route;
import java.util.Optional;
public interface RouteRepository { Optional<Route> findByScenarioId(String scenarioId); }
