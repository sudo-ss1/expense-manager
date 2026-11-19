package com.iconcile.expenses.config;

import com.iconcile.expenses.domain.AnomalyDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public AppConfig(AppProperties props) {
        this.props = props;
    }

    @Bean
    public AnomalyDetector anomalyDetector() {
        return new AnomalyDetector(props.anomaly().multiplier(), props.anomaly().minPeers());
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
