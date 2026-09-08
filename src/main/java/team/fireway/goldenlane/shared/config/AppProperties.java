package team.fireway.goldenlane.shared.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;
@ConfigurationProperties("app")
public record AppProperties(External external,Cors cors) { public record External(String fireSystemUrl){} public record Cors(List<String> allowedOrigins){} }

