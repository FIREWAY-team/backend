package team.fireway.goldenlane.modules.vehicles;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/vehicles")
public class VehicleController { private final JdbcTemplate db; public VehicleController(JdbcTemplate db){this.db=db;}
 @GetMapping public List<Map<String,Object>> list(){return db.queryForList("SELECT vehicle_id,name,width_m,height_m,length_m,weight_ton,turning_radius_m FROM vehicles ORDER BY vehicle_id");}
}

