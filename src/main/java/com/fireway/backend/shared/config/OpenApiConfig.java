package com.fireway.backend.shared.config;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.*;
@Configuration public class OpenApiConfig { @Bean OpenAPI firewayOpenAPI() { return new OpenAPI().info(new Info().title("Fireway Backend API").version("v2").description("Fireway emergency routing API")).servers(List.of(new Server().url("http://localhost:8080"), new Server().url("https://fireroad.shop"))); } }
