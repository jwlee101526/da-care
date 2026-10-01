package com.dacare.server.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private static final String[] DEFAULT_DEVELOPMENT_ORIGINS = {"http://localhost:3000",
      "http://localhost:5173"};

  private final String[] allowedOrigins;

  public WebConfig(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
    String[] origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isBlank())
        .toArray(String[]::new);
    this.allowedOrigins = origins.length == 0 ? DEFAULT_DEVELOPMENT_ORIGINS : origins;
  }

  @Override
  public void configureApiVersioning(ApiVersionConfigurer configurer) {
    configurer.useRequestHeader("API-Version").setVersionRequired(false);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("Authorization", "Content-Type", "Accept", "API-Version");
  }
}
