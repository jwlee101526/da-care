package com.dacare.server;

import com.dacare.server.service.DiagnosisTools;
import com.dacare.server.service.ReservationService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiagnosisToolsTests {
    private final VectorStore store = mock(VectorStore.class);
    private final ReservationService reservations = mock(ReservationService.class);

    @Test
    void jsonToolCallsProduceCardsOnlyAfterVerifiedManualLookup() {
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder().id("manual-1").text("전원이 켜지지 않으면 전원 어댑터 연결 상태를 확인하세요.").build()));
        var tools = new DiagnosisTools(store, reservations, "노트북 전원이 안 켜져요", null);
        var callbacks = Arrays.stream(ToolCallbacks.from(tools)).collect(java.util.stream.Collectors.toMap(
                callback -> callback.getToolDefinition().name(), callback -> callback));

        assertTrue(tools.cards().isEmpty());
        String search = callbacks.get("searchManuals").call("{\"query\":\"노트북 전원\"}");
        assertTrue(search.contains("manual-1"));
        String inspection = callbacks.get("showInspectionCard").call("""
                {"title":"전원 연결 점검","deviceType":"laptop","deviceName":"노트북",
                 "suspectedCause":"전원 어댑터 연결 상태 확인 필요","inspectionDetails":"전원 어댑터 연결 상태를 확인하세요.",
                 "sourceIds":["manual-1"]}
                """);
        assertTrue(inspection.contains("\"type\":\"inspection\""));
        assertEquals("전원이 켜지지 않으면 전원 어댑터 연결 상태를 확인하세요.", ((DiagnosisTools.InspectionCard) tools.cards().getFirst()).evidence().getFirst().quote());
        callbacks.get("prepareReservation").call("{\"deviceType\":\"laptop\"}");
        assertEquals(2, tools.cards().size());
        var booking = (DiagnosisTools.BookingCard) tools.cards().get(1);
        assertEquals("노트북 전원이 안 켜져요", booking.symptom());
        assertTrue(booking.loginRequired());
        assertEquals(List.of("searchManuals", "showInspectionCard", "prepareReservation"), tools.executedTools());
        verifyNoInteractions(reservations);
    }

    @Test
    void fabricatedEvidenceCannotProduceInspectionCard() {
        var tools = new DiagnosisTools(store, reservations, "세탁기 소음", null);
        assertThrows(IllegalArgumentException.class, () -> tools.showInspectionCard("점검", DiagnosisTools.DeviceType.appliance,
                "세탁기", "댐퍼 불량", "댐퍼 교체", List.of("invented")));
        assertTrue(tools.cards().isEmpty());
    }

    @Test
    void sourceIdMustMatchRetrievedSource() {
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(Document.builder().id("manual-1").text("전원 연결 확인").build()));
        var tools = new DiagnosisTools(store, reservations, "전원 문제", null);
        tools.searchManuals("전원");
        assertThrows(IllegalArgumentException.class, () -> tools.showInspectionCard("점검", DiagnosisTools.DeviceType.laptop,
                "노트북", "배터리 불량", "배터리 교체", List.of("manual-2")));
        assertTrue(tools.cards().isEmpty());
    }

    @Test
    void emptySearchDoesNotGenerateDiagnosis() {
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        var tools = new DiagnosisTools(store, reservations, "알 수 없는 증상", null);
        assertTrue(tools.searchManuals("알 수 없는 증상").isEmpty());
        assertTrue(tools.cards().isEmpty());
        tools.prepareReservation(DiagnosisTools.DeviceType.etc);
        assertEquals(1, tools.cards().size());
        assertInstanceOf(DiagnosisTools.BookingCard.class, tools.cards().getFirst());
    }

    @Test
    void missingCauseAndDocumentIdAreNotPresentedAsDiagnosis() {
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(Document.builder().id("manual-1").text("전원 연결 확인").build()));
        var tools = new DiagnosisTools(store, reservations, "전원 문제", null);
        tools.searchManuals("전원");
        assertNull(tools.showInspectionCard("점검", DiagnosisTools.DeviceType.laptop, "노트북", null, "전원 연결 확인", List.of("manual-1")).suspectedCause());
        assertNull(tools.showInspectionCard("점검", DiagnosisTools.DeviceType.laptop, "노트북", "manual-1", "전원 연결 확인", List.of("manual-1")).suspectedCause());
    }

    @Test
    void unavailableSearchDoesNotPretendToSucceed() {
        var tools = new DiagnosisTools(null, reservations, "증상", null);
        assertThrows(IllegalStateException.class, () -> tools.searchManuals("증상"));
        assertTrue(tools.executedTools().isEmpty());
        assertTrue(tools.cards().isEmpty());
    }

    @Test
    void anonymousUserCannotQueryReservations() {
        var tools = new DiagnosisTools(store, reservations, "예약 조회", null);
        assertThrows(IllegalStateException.class, () -> tools.getReservationStatus(1));
        verifyNoInteractions(reservations);
        assertTrue(tools.cards().isEmpty());
    }

    @Test
    void reservationLookupUsesServerAuthenticatedIdentity() {
        when(reservations.mineOne("owner@test.local", 2L)).thenThrow(new NoSuchElementException("예약을 찾을 수 없습니다."));
        var tools = new DiagnosisTools(store, reservations, "다른 사용자 예약 조회", "owner@test.local");
        assertThrows(NoSuchElementException.class, () -> tools.getReservationStatus(2));
        verify(reservations).mineOne("owner@test.local", 2L);
        assertTrue(tools.cards().isEmpty());
    }

    @Test
    void navigationToolCreatesOnlyAnAllowedPageCard() {
        var tools = new DiagnosisTools(store, reservations, "예약 현황", null);
        var card = tools.navigateTo(DiagnosisTools.Page.reservations);
        assertEquals("예약 내역 조회", card.title());
        assertEquals("예약 내역으로 이동", card.actionLabel());
        assertEquals(List.of("navigateTo"), tools.executedTools());
        assertEquals(card, tools.cards().getFirst());
    }
}
