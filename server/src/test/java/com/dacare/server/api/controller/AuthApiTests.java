package com.dacare.server.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 인증 실패와 이메일 중복이 각각 401, 409와 오류 코드로 구분되어 응답되는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTests {

  @Autowired
  private MockMvc mvc;

  @Test
  void rejectsDuplicateEmailWithConflict() throws Exception {
    String email = "duplicate@test.local";
    signup(email).andExpect(status().isOk());

    signup(email)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
  }

  @Test
  void rejectsWrongPasswordAndUnknownEmailAlike() throws Exception {
    String email = "login@test.local";
    signup(email).andExpect(status().isOk());

    login(email, "wrong-password")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    login("unknown@test.local", "password1234")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  private ResultActions signup(String email) throws Exception {
    return mvc.perform(post("/api/auth/signup").header("API-Version", "1")
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"email":"%s","password":"password1234","name":"홍길동",
             "phone":"010-1234-5678","address":"서울특별시 강남구 테헤란로 1"}
            """.formatted(email)));
  }

  private ResultActions login(String email, String password) throws Exception {
    return mvc.perform(post("/api/auth/login").header("API-Version", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
  }
}
