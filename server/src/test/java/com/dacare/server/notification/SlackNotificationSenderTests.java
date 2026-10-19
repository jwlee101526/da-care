package com.dacare.server.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dacare.server.domain.DeviceType;
import com.dacare.server.domain.NotificationHistory;
import com.dacare.server.domain.NotificationStatus;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.domain.Reservation;
import com.dacare.server.domain.ReservationCode;
import com.dacare.server.repository.NotificationHistoryRepository;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SlackNotificationSenderTests {

  private final NotificationHistoryRepository histories = mock(NotificationHistoryRepository.class);

  @Test
  void recordsFailureWhenWebhookDoesNotRespondInTime() throws Exception {
    // 연결은 받아 두고 응답하지 않는 서버
    try (ServerSocket server = new ServerSocket(0)) {
      SlackNotificationSender sender = new SlackNotificationSender(
          "http://127.0.0.1:" + server.getLocalPort() + "/hook", Duration.ofMillis(300),
          histories, Clock.systemDefaultZone());

      long startedAt = System.nanoTime();
      sender.sendReservationReceived(reservation());

      assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(5));
    }
    ArgumentCaptor<NotificationHistory> history = ArgumentCaptor.forClass(
        NotificationHistory.class);
    verify(histories).save(history.capture());
    assertThat(history.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
  }

  private static Reservation reservation() {
    Reservation reservation = mock(Reservation.class);
    when(reservation.getCode()).thenReturn(ReservationCode.parse("48217390").orElseThrow());
    when(reservation.getContactName()).thenReturn("홍길동");
    when(reservation.getContactPhone()).thenReturn(PhoneNumber.ofMobile("010-1234-5678"));
    when(reservation.getVisitAddress()).thenReturn("서울특별시 강남구 테헤란로 1");
    when(reservation.getDeviceType()).thenReturn(DeviceType.washing);
    when(reservation.getSymptomDescription()).thenReturn("탈수 시 소음");
    when(reservation.getPreferredAt()).thenReturn(LocalDateTime.of(2026, 10, 20, 10, 0));
    return reservation;
  }
}
