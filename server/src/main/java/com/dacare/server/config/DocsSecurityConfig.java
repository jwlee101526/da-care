package com.dacare.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("dev")
@Configuration
public class DocsSecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain docsSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher("/docs", "/docs/**", "/v3/api-docs", "/v3/api-docs/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .build();
  }
}
