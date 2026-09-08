package team.fireway.goldenlane.modules.cctv;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/cctv")
public class CctvController { private final JdbcTemplate db; public CctvController(JdbcTemplate db){this.db=db;}
 @GetMapping("/{cctvId}") public Map<String,Object> get(@PathVariable String cctvId){List<Map<String,Object>> r=db.queryForList("SELECT cctv_id,edge_id,still_public_url,wall_width_m,obstacle_width_m,effective_width_m,detected_objects,verdict,confidence,measured_at,method,calibration_error_m,source_meta FROM cctv_readings WHERE cctv_id=?",cctvId); if(r.isEmpty()) throw new team.fireway.goldenlane.shared.exception.NotFoundException("CCTV "+cctvId+" 판독 결과 없음"); return r.getFirst();}
}

