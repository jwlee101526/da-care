package com.dacare.server.config;

import com.dacare.server.domain.Role;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(withDefaults()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll().requestMatchers("/api/auth/**", "/api/diagnosis/**", "/api/reservations/guest/**", "/actuator/health", "/actuator/health/readiness", "/docs", "/docs/**", "/v3/api-docs", "/v3/api-docs/**").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/**", "/actuator/**").authenticated().requestMatchers("/", "/index.html", "/assets/**", "/en", "/ko", "/reserve", "/reservations", "/reservations/new", "/order", "/login", "/signup", "/admin", "/en/reserve", "/en/reservations", "/en/reservations/new", "/en/login", "/en/signup").permitAll().anyRequest().denyAll()).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}

@Component
class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    JwtFilter(JwtService jwtService) { this.jwtService = jwtService; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) try {
            var claims = jwtService.parse(header.substring(7));
            var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class))));
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException ignored) { }
        chain.doFilter(request, response);
    }
}
