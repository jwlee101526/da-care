package com.dacare.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.dacare.server.service.usage.UsageSubject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UsageSubjectResolverTests {

  private final UsageSubjectResolver resolver = new UsageSubjectResolver("True-Client-IP", "secret");

  @Test
  void loggedInUsersAreCountedPerAccountByRole() {
    var customer = new UsernamePasswordAuthenticationToken("demo@dacare.com", null,
        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    var admin = new UsernamePasswordAuthenticationToken("admin@dacare.com", null,
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    assertThat(resolver.resolve(customer, new MockHttpServletRequest()))
        .isEqualTo(UsageSubject.customer("demo@dacare.com"));
    assertThat(resolver.resolve(admin, new MockHttpServletRequest()))
        .isEqualTo(UsageSubject.admin("admin@dacare.com"));
  }

  @Test
  void guestsAreCountedPerHashedClientIp() {
    var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    UsageSubject first = resolver.resolve(anonymous, request("203.0.113.7", "10.0.0.1"));
    UsageSubject sameIpOtherProxy = resolver.resolve(null, request("203.0.113.7", "10.0.0.2"));
    UsageSubject otherIp = resolver.resolve(null, request("198.51.100.4", "10.0.0.1"));

    assertThat(first.isGuest()).isTrue();
    assertThat(first.id()).doesNotContain("203.0.113.7").hasSize(64);
    assertThat(sameIpOtherProxy).isEqualTo(first);
    assertThat(otherIp).isNotEqualTo(first);
  }

  @Test
  void ignoresClientHeaderUnlessConfigured() {
    var withoutHeader = new UsageSubjectResolver("", "secret");

    // 헤더를 지정하지 않으면 클라이언트가 보낸 헤더 대신 원격 주소로 센다.
    assertThat(withoutHeader.resolve(null, request("203.0.113.7", "10.0.0.1")))
        .isEqualTo(withoutHeader.resolve(null, request("198.51.100.4", "10.0.0.1")));
  }

  private static MockHttpServletRequest request(String clientIpHeader, String remoteAddr) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("True-Client-IP", clientIpHeader);
    request.setRemoteAddr(remoteAddr);
    return request;
  }
}
