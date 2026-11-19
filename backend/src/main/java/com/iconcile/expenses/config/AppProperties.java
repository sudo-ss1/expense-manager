package com.iconcile.expenses.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Anomaly anomaly, Cors cors) {

    public record Anomaly(BigDecimal multiplier, int minPeers) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
