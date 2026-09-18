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
                                {"deviceType":"LAPTOP","symptomDescription":"전원이 켜지지 않습니다.","contactName":"방문 연락처","contactPhone":"010-9999-1234","visitAddress":"서울시 강남구","preferredAt":"2099-01-01T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.contactName").value("방문 연락처"))
                .andExpect(jsonPath("$.contactPhone").value("010-9999-1234"));

        mockMvc.perform(get("/api/reservations/me")
                        .header("API-Version", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceType").value("LAPTOP"))
                .andExpect(jsonPath("$[0].contactPhone").value("010-9999-1234"));

        mockMvc.perform(get("/api/admin/reservations")
                        .header("API-Version", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanCancelPendingReservationButCannotCancelItTwice() throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup").header("API-Version", "1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cancel@test.local\",\"password\":\"customer-password\",\"name\":\"Customer\",\"phone\":\"010-1234-5678\",\"address\":\"Seoul\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
        String reservation = mockMvc.perform(post("/api/reservations").header("API-Version", "1").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceType\":\"laptop\",\"symptomDescription\":\"Will not power on\",\"visitAddress\":\"Seoul\",\"preferredAt\":\"2099-01-01T10:00:00\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = reservation.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");
        mockMvc.perform(patch("/api/reservations/" + id + "/cancel").header("API-Version", "1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(patch("/api/reservations/" + id + "/cancel").header("API-Version", "1").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test
    void anonymousReservationIsRejected() throws Exception {
        mockMvc.perform(post("/api/reservations").header("API-Version", "1").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"deviceType":"laptop","symptomDescription":"전원 문제","visitAddress":"서울","preferredAt":"2099-01-01T10:00:00"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidDateIsNotSavedAndOtherCustomersCannotReadReservation() throws Exception {
        String owner = signup("owner@test.local");
        String other = signup("other@test.local");
        mockMvc.perform(post("/api/reservations").header("API-Version", "1").header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"deviceType":"laptop","symptomDescription":"전원 문제","visitAddress":"서울","preferredAt":"2000-01-01T10:00:00"}
                        """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reservations/me").header("API-Version", "1").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        String response = mockMvc.perform(post("/api/reservations").header("API-Version", "1").header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"deviceType":"laptop","symptomDescription":"전원 문제","visitAddress":"서울","preferredAt":"2099-01-01T10:00:00"}
                        """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");
        mockMvc.perform(get("/api/reservations/" + id).header("API-Version", "1").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void guestCanCreateLookupAndCancelReservation() throws Exception {
        String response = mockMvc.perform(post("/api/reservations/guest")
                        .header("API-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceType":"fridge","symptomDescription":"냉각이 되지 않음","visitAddress":"서울시 서초구 방배동 100","preferredAt":"2099-01-01T14:00:00","contactName":"비회원손님","contactPhone":"010-5555-6666","guestPassword":"1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.contactName").value("비회원손님"))
                .andExpect(jsonPath("$.contactPhone").value("010-5555-6666"))
                .andReturn().getResponse().getContentAsString();
        String id = response.replaceAll(".*\\\"id\\\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/reservations/guest/lookup")
                        .header("API-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":" + id + ",\"contactPhone\":\"010-5555-6666\",\"guestPassword\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.deviceType").value("fridge"));

        mockMvc.perform(patch("/api/reservations/guest/" + id + "/cancel")
                        .header("API-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactPhone\":\"010-5555-6666\",\"guestPassword\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private String signup(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup").header("API-Version", "1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"customer-password\",\"name\":\"고객\",\"phone\":\"010-1234-5678\",\"address\":\"서울\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return response.replaceAll(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
