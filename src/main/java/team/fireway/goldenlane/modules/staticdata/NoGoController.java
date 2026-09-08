package team.fireway.goldenlane.modules.staticdata;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/no_go")
public class NoGoController { private final JdbcTemplate db; public NoGoController(JdbcTemplate db){this.db=db;}
 @GetMapping public Map<String,Object> find(@RequestParam String bbox,@RequestParam(required=false) Integer layer){String[] p=bbox.split(","); if(p.length!=4) throw new IllegalArgumentException("bbox must be minLon,minLat,maxLon,maxLat"); double minLon=Double.parseDouble(p[0]),minLat=Double.parseDouble(p[1]),maxLon=Double.parseDouble(p[2]),maxLat=Double.parseDouble(p[3]); String sql="SELECT id,dong,reason,layer,ST_AsGeoJSON(polygon) geometry FROM no_go_areas WHERE ST_Intersects(polygon,ST_GeomFromText(?,4326))"+(layer==null?"":" AND layer=?"); String wkt=String.format(Locale.ROOT,"POLYGON((%f %f,%f %f,%f %f,%f %f,%f %f))",minLon,minLat,maxLon,minLat,maxLon,maxLat,minLon,maxLat,minLon,minLat); List<Map<String,Object>> rows=layer==null?db.queryForList(sql,wkt):db.queryForList(sql,wkt,layer); return Map.of("type","FeatureCollection","features",rows);}
}

