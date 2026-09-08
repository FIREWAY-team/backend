package com.fireway.backend.shared.config;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;
@Configuration public class CorsConfig { @Bean WebMvcConfigurer corsConfigurer() { return new WebMvcConfigurer() { public void addCorsMappings(CorsRegistry r) { r.addMapping("/**").allowedOrigins("https://fireroad.shop", "http://localhost:3000").allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*"); } }; } }
