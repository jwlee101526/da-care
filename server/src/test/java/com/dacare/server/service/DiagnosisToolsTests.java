package com.dacare.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dacare.server.config.DiagnosisToolConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;

/**
 * 모델·DB 없이 진단 도구의 오류 전달과 취소 동작을 검증한다.
 */
class DiagnosisToolsTests {

  private final ToolExecutionExceptionProcessor processor = new DiagnosisToolConfig()
      .toolExecutionExceptionProcessor(false);
  private final ToolDefinition definition = ToolDefinition.builder().name("searchManuals")
      .description("test").inputSchema("{}").build();

  private DiagnosisTools tools() {
    return new DiagnosisTools(null, null, null, 0.4, ignored -> {
    });
  }

  @Test
  void toolFailureIsReturnedToModelAsMessage() {
    String message = processor.process(
        new ToolExecutionException(definition, new IllegalArgumentException("도구 입력값을 확인하세요.")));

    assertEquals("도구 입력값을 확인하세요.", message);
  }

  @Test
  void cancellationIsRethrownInsteadOfReturnedToModel() {
    assertThrows(DiagnosisCancelledException.class, () -> processor.process(
        new ToolExecutionException(definition, new DiagnosisCancelledException())));
  }

  @Test
  void bookingCardUsesSymptomSummarizedByModel() {
    DiagnosisCard.BookingCard card = tools().prepareReservation(DiagnosisTools.DeviceType.laptop,
        " 충전 표시등이 깜빡이고 전원이 켜지지 않습니다. ");

    assertEquals("충전 표시등이 깜빡이고 전원이 켜지지 않습니다.", card.symptom());
    assertEquals(true, card.loginRequired());
  }

  @Test
  void bookingCardRequiresSymptom() {
    assertThrows(IllegalArgumentException.class,
        () -> tools().prepareReservation(DiagnosisTools.DeviceType.laptop, " "));
  }

  @Test
  void reservationRequestGuideNavigatesToNewReservationPage() {
    DiagnosisCard.NavigationCard card = tools().navigateTo(DiagnosisTools.Page.reserve);

    assertEquals(DiagnosisTools.Page.reserve, card.page());
    assertEquals("방문 점검 신청", card.title());
  }

  @Test
  void interruptedRequestStopsBeforeRunningTool() {
    Thread.currentThread().interrupt();
    try {
      assertThrows(DiagnosisCancelledException.class,
          () -> tools().searchManuals("노트북 전원"));
    } finally {
      Thread.interrupted();
    }
  }
}
