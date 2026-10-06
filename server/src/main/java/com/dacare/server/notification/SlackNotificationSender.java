package com.dacare.server.notification;

import static com.dacare.server.domain.NotificationChannel.SLACK;
import static com.dacare.server.domain.NotificationStatus.FAILED;
import static com.dacare.server.domain.NotificationStatus.SKIPPED;
import static com.dacare.server.domain.NotificationStatus.SUCCESS;

import com.dacare.server.domain.NotificationHistory;
import com.dacare.server.domain.NotificationStatus;
import com.dacare.server.domain.Reservation;
import com.dacare.server.domain.ReservationCode;
import com.dacare.server.repository.NotificationHistoryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackNotificationSender {

  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(
      "yyyy년 M월 d일 (E) HH:mm");

  private final String webhookUrl;
  private final NotificationHistoryRepository histories;
  private final Clock clock;
  private final RestClient client = RestClient.create();

  public SlackNotificationSender(
      @Value("${app.slack.webhook-url:}") String webhookUrl,
      NotificationHistoryRepository histories, Clock clock) {
    this.webhookUrl = webhookUrl;
    this.histories = histories;
    this.clock = clock;
  }

  public void sendReservationReceived(Reservation reservation) {
    String message = message(reservation);
    if (webhookUrl.isBlank()) {
      record(reservation, SKIPPED, message, "SLACK_WEBHOOK_URL 미설정");
      return;
    }

    try {
      client.post().uri(webhookUrl).body(Map.of("text", message)).retrieve().toBodilessEntity();
      record(reservation, SUCCESS, message, null);
    } catch (RuntimeException e) {
      record(reservation, FAILED, message, e.getMessage());
    }
  }

  private void record(Reservation reservation, NotificationStatus status, String message,
      String failureReason) {
    histories.save(new NotificationHistory(reservation, SLACK, status, message, failureReason,
        LocalDateTime.now(clock)));
  }

  private String message(Reservation reservation) {
    return "🔔 *새 수리 예약이 접수되었습니다*\n"
        + "━━━━━━━━━━━━━━━━━━\n"
        + "*접수 번호*  " + ReservationCode.format(reservation.getCode()) + " (#" + reservation.getId()
        + ")\n"
        + "*진행 상태*  배정 대기\n\n"
        + "*고객 정보*\n"
        + "• 성함: " + reservation.getContactName() + "\n"
        + "• 연락처: " + reservation.getContactPhone().format() + "\n"
        + "• 방문 주소: " + reservation.getVisitAddress() + "\n\n"
        + "*수리 요청*\n"
        + "• 품목: " + reservation.getDeviceType().label() + "\n"
        + "• 증상: " + reservation.getSymptomDescription() + "\n"
        + "• 희망 방문: " + reservation.getPreferredAt().format(DATE_TIME_FORMAT) + "\n";
  }
}
