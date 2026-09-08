package team.fireway.goldenlane.modules.routing.infrastructure;
import jakarta.persistence.Entity; import jakarta.persistence.Id; import jakarta.persistence.Table;
@Entity @Table(name="scenarios") public class RouteProjection { @Id private String scenarioId; protected RouteProjection(){} public String getScenarioId(){return scenarioId;} }

