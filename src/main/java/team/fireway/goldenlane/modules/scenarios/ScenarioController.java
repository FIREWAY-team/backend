package team.fireway.goldenlane.modules.scenarios;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/scenarios")
public class ScenarioController { private final JdbcTemplate db; public ScenarioController(JdbcTemplate db){this.db=db;}
 @GetMapping public List<Map<String,Object>> list(){return db.queryForList("SELECT scenario_id,title,fire_lat,fire_lon,vehicle_hint,routes_precomputed,explanation_precomputed,created_at FROM scenarios ORDER BY scenario_id");}
}

