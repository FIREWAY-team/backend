package team.fireway.goldenlane.modules.routing.application;
import team.fireway.goldenlane.modules.routing.application.port.RouteRepository;
import org.springframework.stereotype.Service;
@Service public class RouteService { private final RouteRepository repository; public RouteService(RouteRepository repository){this.repository=repository;} public String find(String scenarioId){return repository.findPrecomputed(scenarioId).orElseThrow();} }

