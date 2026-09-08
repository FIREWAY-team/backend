package team.fireway.goldenlane.modules.routing.application.port;
import java.util.Optional;
public interface RouteRepository { Optional<String> findPrecomputed(String scenarioId); }

