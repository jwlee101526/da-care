package com.dacare.server.security;

import com.dacare.server.api.error.ErrorResponse;
import com.dacare.server.domain.Role;
import com.dacare.server.web.SpaRoutes;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

  private static final String[] PUBLIC_API = {"/api/auth/**", "/api/diagnosis/**",
      "/api/reservations/guest/**", "/api/usage", "/actuator/health",
      "/actuator/health/readiness"};

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
      JsonMapper jsonMapper) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(PUBLIC_API).permitAll()
            .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
            .requestMatchers("/api/**", "/actuator/**").authenticated()
            .requestMatchers("/index.html", "/assets/**").permitAll()
            .requestMatchers(SpaRoutes.PATHS).permitAll()
            .anyRequest().denyAll())
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, exception) -> writeError(response,
                jsonMapper, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."))
            .accessDeniedHandler((request, response, exception) -> writeError(response,
                jsonMapper, HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.")))
        .addFilterBefore(new JwtAuthenticationFilter(jwtService),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private static void writeError(HttpServletResponse response, JsonMapper jsonMapper,
      HttpStatus status, String code, String message) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of(code, message));
  }
}
