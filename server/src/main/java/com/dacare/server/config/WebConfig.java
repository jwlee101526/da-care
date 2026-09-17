package com.dacare.server.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override public void configureApiVersioning(ApiVersionConfigurer configurer) { configurer.useRequestHeader("API-Version"); }
}
