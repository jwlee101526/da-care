package com.dacare.server.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final Duration expiration;

  public JwtService(@Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration}") Duration expiration) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = expiration;
  }

  public String createToken(String email, String role) {
    return Jwts.builder().subject(email).claim("role", role).issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration.toMillis())).signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
