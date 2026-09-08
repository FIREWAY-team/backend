package com.fireway.backend.shared.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "app") public record AppProperties(External external) {
    public record External(String fireSystemUrl) { }
}
