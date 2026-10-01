package com.dacare.server.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 인증·인가 실패가 프론트가 해석할 수 있는 JSON 오류로 응답되고, SPA 라우트는 index.html로 포워딩되는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {

  @Autowired
  private MockMvc mvc;
  @Autowired
  private JwtService jwt;

  @Test
  void anonymousRequestToProtectedApiReturnsUnauthorizedJson() throws Exception {
    mvc.perform(get("/api/reservations/me").header("API-Version", "1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void invalidTokenIsTreatedAsAnonymous() throws Exception {
    mvc.perform(get("/api/reservations/me").header("API-Version", "1")
            .header("Authorization", "Bearer invalid"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void customerTokenCannotAccessAdminApi() throws Exception {
    mvc.perform(get("/api/admin/reservations").header("API-Version", "1")
            .header("Authorization", "Bearer " + jwt.createToken("someone@test.local", "CUSTOMER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void guestCanReadOwnWeeklyUsage() throws Exception {
    mvc.perform(get("/api/usage").header("API-Version", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.diagnosis.limit").value(5));
  }

  @Test
  void totalUsageIsAdminOnly() throws Exception {
    mvc.perform(get("/api/admin/usage").header("API-Version", "1")
            .header("Authorization", "Bearer " + jwt.createToken("someone@test.local", "CUSTOMER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/usage").header("API-Version", "1")
            .header("Authorization", "Bearer " + jwt.createToken("admin@test.local", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sms.limit").value(15));
  }

  @Test
  void spaRouteIsForwardedToIndex() throws Exception {
    mvc.perform(get("/en/reservations")).andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }
}
