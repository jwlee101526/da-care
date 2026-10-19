package com.dacare.server.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificationHistoryTests {

  @Test
  void truncatesMessageAndFailureReasonToColumnLength() {
    NotificationHistory history = history("가".repeat(2100), "x".repeat(300));

    assertThat(history.getMessage()).hasSize(NotificationHistory.MESSAGE_MAX_LENGTH);
    assertThat(history.getFailureReason()).hasSize(NotificationHistory.FAILURE_REASON_MAX_LENGTH);
  }

  @Test
  void keepsShortTextAsIs() {
    NotificationHistory history = history("알림", null);

    assertThat(history.getMessage()).isEqualTo("알림");
    assertThat(history.getFailureReason()).isNull();
  }

  @Test
  void doesNotSplitSurrogatePair() {
    String message = "가".repeat(NotificationHistory.MESSAGE_MAX_LENGTH - 1) + "🔔";

    assertThat(history(message, null).getMessage())
        .isEqualTo("가".repeat(NotificationHistory.MESSAGE_MAX_LENGTH - 1));
  }

  private static NotificationHistory history(String message, String failureReason) {
    return new NotificationHistory(null, NotificationChannel.SLACK, NotificationStatus.FAILED,
        message, failureReason, LocalDateTime.of(2026, 10, 1, 10, 0));
  }
}
