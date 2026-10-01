package com.dacare.server.web;

import com.dacare.server.service.UsageSubject;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 요청을 AI 상담 사용량 집계 단위로 바꾼다. 로그인 사용자는 계정, 비로그인 사용자는 클라이언트 IP로 센다.
 * <p>
 * 비로그인 한도는 비용 보호용이라 클라이언트가 지우거나 조작할 수 있는 쿠키·브라우저 식별자 대신 서버가 관찰한 IP를 쓴다.
 * IP는 개인정보이므로 원문 대신 서버 비밀값으로 만든 HMAC으로 저장한다.
 */
@Component
public class UsageSubjectResolver {

  private static final String HMAC = "HmacSHA256";

  private final String clientIpHeader;
  private final SecretKeySpec hashKey;

  /**
   * @param clientIpHeader 앞단 프록시가 덮어써 클라이언트가 위조할 수 없는 실제 IP 헤더(예: Render의 True-Client-IP).
   *                       비어 있으면 서블릿 컨테이너가 신뢰 프록시 체인을 거쳐 구한 원격 주소를 쓴다.
   */
  public UsageSubjectResolver(@Value("${app.usage.client-ip-header:}") String clientIpHeader,
      @Value("${app.usage.ip-hash-secret}") String hashSecret) {
    this.clientIpHeader = clientIpHeader.trim();
    this.hashKey = new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), HMAC);
  }

  public UsageSubject resolve(Authentication authentication, HttpServletRequest request) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      return UsageSubject.admin(authentication.getName());
    }
    if (hasRole(authentication, "ROLE_CUSTOMER")) {
      return UsageSubject.customer(authentication.getName());
    }
    return UsageSubject.guest(hash(clientIp(request)));
  }

  private static boolean hasRole(Authentication authentication, String role) {
    return authentication != null && authentication.isAuthenticated()
        && authentication.getAuthorities().stream()
        .anyMatch(authority -> role.equals(authority.getAuthority()));
  }

  private String clientIp(HttpServletRequest request) {
    if (!clientIpHeader.isEmpty()) {
      String header = request.getHeader(clientIpHeader);
      if (header != null && !header.isBlank()) {
        return header.trim();
      }
    }
    return request.getRemoteAddr();
  }

  private String hash(String ip) {
    try {
      Mac mac = Mac.getInstance(HMAC);
      mac.init(hashKey);
      return HexFormat.of().formatHex(mac.doFinal(ip.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException("IP 해시를 만들 수 없습니다.", exception);
    }
  }
}
