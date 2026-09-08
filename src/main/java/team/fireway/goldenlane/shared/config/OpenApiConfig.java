package team.fireway.goldenlane.shared.config;
import io.swagger.v3.oas.models.OpenAPI; import io.swagger.v3.oas.models.info.Info; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
@Configuration public class OpenApiConfig { @Bean OpenAPI goldenLaneOpenAPI(){return new OpenAPI().info(new Info().title("Golden Lane API").version("v1").description("성남 구도심 소방차 진입 판단 엔진"));} }

