package com.fireway.backend.modules.scenarios.interfaces;
import java.util.List;
import com.fireway.backend.modules.scenarios.application.ScenarioService;
import com.fireway.backend.modules.scenarios.domain.Scenario;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/scenarios") public class ScenarioController {
    private final ScenarioService service; public ScenarioController(ScenarioService service) { this.service = service; }
    @GetMapping public List<ScenarioResponse> list() { return service.mock().stream().map(s -> new ScenarioResponse(s.scenarioId(), s.title(), s.fireLat(), s.fireLon(), s.vehicleHint())).toList(); }
    public record ScenarioResponse(String scenarioId, String title, double fireLat, double fireLon, String vehicleHint) { }
}
