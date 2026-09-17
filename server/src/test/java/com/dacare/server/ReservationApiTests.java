package com.dacare.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reservation-api;MODE=MariaDB;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.mariadb.autoconfigure.MariaDbStoreAutoConfiguration",
        "spring.ai.openai.api-key=test-key",
        "app.jwt.secret=test-secret-that-is-long-enough-for-hmac-signing-key",
        "app.jwt.expiration=PT8H",
        "app.admin.email=admin@test.local",
        "app.admin.password=test-admin-password",
        "app.slack.webhook-url="
})
@AutoConfigureMockMvc
class ReservationApiTests {
    @Autowired private MockMvc mockMvc;

    @Test
    void customerCanCreateOwnReservationButCannotAccessAdminApi() throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup")
                        .header("API-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"customer@test.local","password":"customer-password","name":"고객","phone":"010-1234-5678","address":"서울시 강남구"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/reservations")
                        .header("API-Version", "1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceType":"LAPTOP","symptomDescription":"전원이 켜지지 않습니다.","visitAddress":"서울시 강남구","preferredAt":"2099-01-01T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/reservations/me")
                        .header("API-Version", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceType").value("LAPTOP"));

        mockMvc.perform(get("/api/admin/reservations")
                        .header("API-Version", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
