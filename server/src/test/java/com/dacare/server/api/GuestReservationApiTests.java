package com.dacare.server.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 비회원 예약을 예약 번호·휴대전화 번호·비밀번호로만 조회·취소할 수 있고, 실패 사유가 드러나지 않으며 반복 실패가 막히는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GuestReservationApiTests {

  private static final String PHONE = "+821012345678";
  private static final String OTHER_PHONE = "+821087654321";

  @Autowired
  private MockMvc mvc;

  @Test
  void guestPasswordIsRequiredOnCreate() throws Exception {
    mvc.perform(post("/api/reservations/guest").header("API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON).content(createBody("")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.guestPassword").exists());
    mvc.perform(post("/api/reservations/guest").header("API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON).content(createBody("12345")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void looksUpAndCancelsWithMatchingCredentials() throws Exception {
    long id = createGuest("4821");

    lookup(id, PHONE, "4821", "10.0.0.1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.status").value("PENDING"));

    mvc.perform(patch("/api/reservations/guest/{id}/cancel", id).header("API-Version", "1")
            .with(request -> {
              request.setRemoteAddr("10.0.0.1");
              return request;
            })
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"contactPhone\":\"%s\",\"guestPassword\":\"4821\"}".formatted(PHONE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void wrongPhoneAndWrongPasswordAreIndistinguishable() throws Exception {
    long id = createGuest("4821");

    String wrongPassword = lookup(id, PHONE, "0000", "10.0.0.2")
        .andExpect(status().isNotFound())
        .andReturn().getResponse().getContentAsString();
    String wrongPhone = lookup(id, OTHER_PHONE, "4821", "10.0.0.2")
        .andExpect(status().isNotFound())
        .andReturn().getResponse().getContentAsString();
    String missing = lookup(id + 10_000, PHONE, "4821", "10.0.0.2")
        .andExpect(status().isNotFound())
        .andReturn().getResponse().getContentAsString();

    assertThat(wrongPassword)
        .isEqualTo(wrongPhone).isEqualTo(missing);
  }

  @Test
  void repeatedFailuresLockReservationEvenWithCorrectPassword() throws Exception {
    long id = createGuest("4821");
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_RESERVATION; i++) {
      lookup(id, PHONE, "%04d".formatted(i), "10.0.1." + i).andExpect(status().isNotFound());
    }

    lookup(id, PHONE, "4821", "10.0.0.3")
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
  }

  private long createGuest(String guestPassword) throws Exception {
    String body = mvc.perform(post("/api/reservations/guest").header("API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON).content(createBody(guestPassword)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private ResultActions lookup(long id, String phone, String guestPassword, String ip)
      throws Exception {
    return mvc.perform(post("/api/reservations/guest/lookup").header("API-Version", "1")
        .with(request -> {
          request.setRemoteAddr(ip);
          return request;
        })
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"reservationId\":%d,\"contactPhone\":\"%s\",\"guestPassword\":\"%s\"}"
            .formatted(id, phone, guestPassword)));
  }

  private static String createBody(String guestPassword) {
    LocalDateTime preferredAt = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS);
    return """
        {"deviceType":"washer","symptomDescription":"탈수 시 소음","visitAddress":"서울시 강남구 1",\
        "preferredAt":"%s","contactName":"홍길동","contactPhone":"%s","guestPassword":"%s"}"""
        .formatted(preferredAt, PHONE, guestPassword);
  }
}
