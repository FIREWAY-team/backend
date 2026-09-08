package team.fireway.goldenlane.modules.routing;
import team.fireway.goldenlane.shared.exception.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*; import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import java.util.*;
@RestController @RequestMapping("/api/route")
public class RouteController { private final JdbcTemplate db; public RouteController(JdbcTemplate db){this.db=db;}
 public record RouteRequest(@NotBlank String scenarioId,@NotBlank String vehicleId){}
 @PostMapping public Map<String,Object> route(@Valid @RequestBody RouteRequest request){
  List<Map<String,Object>> rows=db.queryForList("SELECT scenario_id,title,vehicle_hint,routes_precomputed,explanation_precomputed FROM scenarios WHERE scenario_id=?",request.scenarioId());
  if(rows.isEmpty()) throw new NotFoundException(ErrorCode.ROUTE_NOT_FOUND,"시나리오 "+request.scenarioId()+" 경로 없음");
  Map<String,Object> r=rows.getFirst(); return Map.of("scenario_id",r.get("scenario_id"),"title",r.get("title"),"vehicle_id",request.vehicleId(),"routes",r.get("routes_precomputed"),"explanation",r.get("explanation_precomputed"));
 }
}

