package com.dacare.server.notification;

import static com.dacare.server.domain.NotificationChannel.SLACK;
import static com.dacare.server.domain.NotificationStatus.FAILED;
import static com.dacare.server.domain.NotificationStatus.SKIPPED;
import static com.dacare.server.domain.NotificationStatus.SUCCESS;

import com.dacare.server.domain.NotificationHistory;
import com.dacare.server.domain.NotificationStatus;
import com.dacare.server.domain.Reservation;
import com.dacare.server.repository.NotificationHistoryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackNotificationSender {

  private static final Logger log = LoggerFactory.getLogger(SlackNotificationSender.class);
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(
      "yyyy년 M월 d일 (E) HH:mm");

  private final String webhookUrl;
  private final NotificationHistoryRepository histories;
  private final Clock clock;
  private final RestClient client;

  /**
   * @param timeout 연결과 응답 대기 각각의 제한 시간. 알림은 예약 요청 스레드에서 보내므로 Slack이 응답하지 않아도 예약 응답이 오래 묶이지 않게 한다.
   */
  public SlackNotificationSender(
      @Value("${app.slack.webhook-url:}") String webhookUrl,
      @Value("${app.slack.timeout:5s}") Duration timeout,
      NotificationHistoryRepository histories, Clock clock) {
    this.webhookUrl = webhookUrl;
    this.histories = histories;
    this.clock = clock;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeout);
    requestFactory.setReadTimeout(timeout);
    this.client = RestClient.builder().requestFactory(requestFactory).build();
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
      log.error("Slack 알림 실패: reservation={}", reservation.getId(), e);
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
        + "*접수 번호*  " + reservation.getCode().format() + " (#" + reservation.getId() + ")\n"
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
