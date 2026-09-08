package com.fireway.backend.modules.scenarios.application.port;
import com.fireway.backend.modules.scenarios.domain.Scenario;
import java.util.List;
public interface ScenarioRepository { List<Scenario> findAll(); }
