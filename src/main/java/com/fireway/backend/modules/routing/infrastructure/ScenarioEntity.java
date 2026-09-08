package com.fireway.backend.modules.routing.infrastructure;
import jakarta.persistence.*;
@Entity @Table(name = "scenarios") public class ScenarioEntity {
    @Id @Column(name = "scenario_id", length = 64) private String scenarioId;
    protected ScenarioEntity() { }
    public String getScenarioId() { return scenarioId; }
}
