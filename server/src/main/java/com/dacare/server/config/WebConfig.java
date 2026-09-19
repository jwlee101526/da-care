package com.dacare.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] DEFAULT_DEVELOPMENT_ORIGINS = { "http://localhost:3000", "http://localhost:5173" };

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader("API-Version").setVersionRequired(false);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
        if (origins.length == 0) origins = DEFAULT_DEVELOPMENT_ORIGINS;
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "API-Version");
    }
}
