package com.fireway.backend.modules.scenarios.application;
import com.fireway.backend.modules.scenarios.application.port.ScenarioRepository;
import com.fireway.backend.modules.scenarios.domain.Scenario;
import java.util.List;
import org.springframework.stereotype.Service;
@Service public class ScenarioService { public ScenarioService(ScenarioRepository ignored) { } public List<Scenario> mock() { return List.of(new Scenario("bank-01", "은행1동 화재", 37.4381, 127.1422, "pump-3.5"), new Scenario("sangdaewon-01", "상대원1동 화재", 37.4311, 127.1642, "pump-8"), new Scenario("moran-01", "모란시장 화재", 37.4324, 127.1299, "pump-3.5")); } }
