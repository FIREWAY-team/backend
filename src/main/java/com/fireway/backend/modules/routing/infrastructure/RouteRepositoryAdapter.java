package com.fireway.backend.modules.routing.infrastructure;
import com.fireway.backend.modules.routing.application.port.RouteRepository;
import com.fireway.backend.modules.routing.domain.Route;
import java.util.Optional;
import org.springframework.stereotype.Component;
@Component public class RouteRepositoryAdapter implements RouteRepository { public Optional<Route> findByScenarioId(String id) { return Optional.empty(); } }
