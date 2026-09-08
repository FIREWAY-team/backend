package team.fireway.goldenlane.shared.config;
import org.springframework.context.annotation.Configuration; import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class CorsConfig implements WebMvcConfigurer {
 private final AppProperties props; public CorsConfig(AppProperties props){this.props=props;}
 public void addCorsMappings(CorsRegistry r){r.addMapping("/api/**").allowedOrigins(props.cors().allowedOrigins().toArray(String[]::new)).allowedMethods("GET","POST","OPTIONS").allowedHeaders("*");}
}

