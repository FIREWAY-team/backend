package team.fireway.goldenlane.shared.config;
import com.fasterxml.jackson.databind.ObjectMapper; import com.fasterxml.jackson.databind.PropertyNamingStrategies; import org.springframework.context.annotation.*; import java.util.TimeZone;
@Configuration public class JacksonConfig { @Bean ObjectMapper objectMapper(){return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));} }

