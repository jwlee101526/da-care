package com.dacare.server.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dacare.server.service.GuestLookupThrottle;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 비회원 예약을 예약 번호와 휴대전화 번호로 한 건만 조회·취소할 수 있고, 개인정보는 가려지며, 실패 사유가 드러나지 않고 반복 실패와 오래된
 * 예약 조회가 막히는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GuestReservationApiTests {

  private static final String OTHER_PHONE = "+821087654321";

  @Autowired
  private MockMvc mvc;
  @Autowired
  private JdbcTemplate jdbc;

  @Test
  void createResponseHasCodeButNoSequentialId() throws Exception {
    mvc.perform(post("/api/reservations/guest").header("API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON).content(createBody("+821011110000")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(matchesPattern("\\d{8}")))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.contactName").value("홍길동"));
  }

  @Test
  void looksUpWithMaskedPersonalDataAndCancels() throws Exception {
    String phone = "+821011110001";
    String code = createGuest(phone);

    lookup(code, phone, "10.0.0.1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.contactName").value("홍*동"))
        .andExpect(jsonPath("$.contactPhone").value("010-****-0001"))
        .andExpect(jsonPath("$.visitAddress").value("서울특별시 강남구 ***"));

    mvc.perform(patch("/api/reservations/guest/{code}/cancel", code).header("API-Version", "1")
            .with(request -> {
              request.setRemoteAddr("10.0.0.1");
              return request;
            })
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"contactPhone\":\"%s\"}".formatted(phone)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void acceptsCodeWithHyphenAndNationalPhoneFormat() throws Exception {
    String code = createGuest("+821011110002");

    lookup(code.substring(0, 4) + "-" + code.substring(4), "010-1111-0002", "10.0.0.4")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void wrongPhoneWrongCodeAndSequentialIdAreIndistinguishable() throws Exception {
    String phone = "+821011110003";
    String code = createGuest(phone);
    Long id = jdbc.queryForObject("SELECT id FROM reservation WHERE code = ?", Long.class, code);

    String wrongPhone = lookup(code, OTHER_PHONE, "10.0.0.2")
        .andExpect(status().isNotFound())
        .andReturn().getResponse().getContentAsString();
    String wrongCode = lookup(otherCode(code), phone, "10.0.0.2")
        .andExpect(status().isNotFound())
        .andReturn().getResponse().getContentAsString();
    String sequentialId = lookup(String.valueOf(id), phone, "10.0.0.2")
        .andExpect(status().isNotFound())
        .andReturn().getResponse().getContentAsString();

    assertThat(wrongPhone).isEqualTo(wrongCode).isEqualTo(sequentialId);
  }

  @Test
  void repeatedFailuresOnPhoneLockLookupEvenWithCorrectCode() throws Exception {
    String phone = "+821011110004";
    String code = createGuest(phone);
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_TARGET; i++) {
      lookup("%08d".formatted(i), phone, "10.0.1." + i).andExpect(status().isNotFound());
    }

    lookup(code, phone, "10.0.0.3")
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
  }

  @Test
  void closedReservationIsHiddenAfterLookupPeriod() throws Exception {
    String phone = "+821011110005";
    String recent = createGuest(phone);
    String old = createGuest(phone);
    jdbc.update("UPDATE reservation SET status = 'COMPLETED', closed_at = ? WHERE code = ?",
        LocalDateTime.now().minusDays(89), recent);
    jdbc.update("UPDATE reservation SET status = 'COMPLETED', closed_at = ? WHERE code = ?",
        LocalDateTime.now().minusDays(91), old);

    lookup(recent, phone, "10.0.0.6").andExpect(status().isOk());
    lookup(old, phone, "10.0.0.6").andExpect(status().isNotFound());
  }

  private String createGuest(String phone) throws Exception {
    String body = mvc.perform(post("/api/reservations/guest").header("API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON).content(createBody(phone)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$.code");
  }

  private ResultActions lookup(String code, String phone, String ip) throws Exception {
    return mvc.perform(post("/api/reservations/guest/lookup").header("API-Version", "1")
        .with(request -> {
          request.setRemoteAddr(ip);
          return request;
        })
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"reservationCode\":\"%s\",\"contactPhone\":\"%s\"}".formatted(code, phone)));
  }

  private static String otherCode(String code) {
    char last = code.charAt(code.length() - 1);
    return code.substring(0, code.length() - 1) + (last == '9' ? '0' : (char) (last + 1));
  }

  private static String createBody(String phone) {
    LocalDateTime preferredAt = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS);
    return """
        {"deviceType":"washer","symptomDescription":"탈수 시 소음",\
        "visitAddress":"서울특별시 강남구 테헤란로 1 101동 1203호","preferredAt":"%s",\
        "contactName":"홍길동","contactPhone":"%s"}"""
        .formatted(preferredAt, phone);
  }
}
