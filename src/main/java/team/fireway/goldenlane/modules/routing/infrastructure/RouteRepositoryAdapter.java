package team.fireway.goldenlane.modules.routing.infrastructure;
import team.fireway.goldenlane.modules.routing.application.port.RouteRepository; import java.util.*; import org.springframework.stereotype.Component;
@Component public class RouteRepositoryAdapter implements RouteRepository { public Optional<String> findPrecomputed(String scenarioId){return Optional.empty();} }

