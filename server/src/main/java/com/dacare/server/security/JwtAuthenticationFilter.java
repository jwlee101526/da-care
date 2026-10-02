package com.dacare.server.security;

import com.dacare.server.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer 토큰을 검증해 인증 정보를 설정한다. 토큰이 없거나 유효하지 않으면 익명 요청으로 진행하고, 접근 허용 여부는 인가 규칙이 판단한다.
 * <p>
 * 빈으로 등록하면 서블릿 필터로도 자동 등록되므로 보안 필터 체인에서 직접 생성한다.
 */
class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      try {
        Claims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()));
        var authority = new SimpleGrantedAuthority(
            Role.from(claims.get("role", String.class)).authority());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of(authority)));
      } catch (JwtException | IllegalArgumentException exception) {
        SecurityContextHolder.clearContext();
        log.debug("유효하지 않은 액세스 토큰: {}", exception.getMessage());
      }
    }
    chain.doFilter(request, response);
  }
}
