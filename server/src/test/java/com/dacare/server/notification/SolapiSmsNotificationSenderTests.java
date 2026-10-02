package com.dacare.server.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dacare.server.domain.NotificationHistory;
import com.dacare.server.domain.NotificationStatus;
import com.dacare.server.domain.Reservation;
import com.dacare.server.repository.NotificationHistoryRepository;
import com.dacare.server.service.ApiUsageService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SolapiSmsNotificationSenderTests {

  private final NotificationHistoryRepository histories = mock(NotificationHistoryRepository.class);
  private final ApiUsageService usage = mock(ApiUsageService.class);
  private final SolapiSmsNotificationSender sender = new SolapiSmsNotificationSender("key",
      "secret", "010-0000-0000", histories, usage);

  @Test
  void skipsSendingWhenWeeklyLimitIsReached() {
    when(usage.tryAcquireSms()).thenReturn(false);
    Reservation reservation = mock(Reservation.class, invocation -> null);
    com.dacare.server.domain.Engineer engineer = mock(com.dacare.server.domain.Engineer.class);
    when(engineer.getName()).thenReturn("김기사");
    when(reservation.getEngineer()).thenReturn(engineer);
    when(reservation.getConfirmedAt()).thenReturn(LocalDateTime.of(2026, 10, 1, 10, 0));

    sender.sendReservationConfirmed(reservation);

    ArgumentCaptor<NotificationHistory> history = ArgumentCaptor.forClass(
        NotificationHistory.class);
    verify(histories).save(history.capture());
    assertThat(history.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
    assertThat(history.getValue().getFailureReason()).isEqualTo("주간 SMS 한도 초과");
  }
}
