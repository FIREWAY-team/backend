package com.fireway.backend.modules.scenarios.infrastructure;
import com.fireway.backend.modules.scenarios.application.port.ScenarioRepository;
import com.fireway.backend.modules.scenarios.domain.Scenario;
import java.util.List;
import org.springframework.stereotype.Component;
@Component public class ScenarioRepositoryAdapter implements ScenarioRepository { public List<Scenario> findAll() { return List.of(); } }
