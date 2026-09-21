package com.dacare.server.notification;

import com.dacare.server.domain.NotificationHistory;
import com.dacare.server.domain.Reservation;
import com.dacare.server.repository.NotificationHistoryRepository;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SolapiSmsNotificationSender {

  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm");
  private final String apiKey;
  private final String apiSecret;
  private final String sender;
  private final NotificationHistoryRepository histories;

  public SolapiSmsNotificationSender(
      @Value("${app.solapi.api-key:}") String apiKey,
      @Value("${app.solapi.api-secret:}") String apiSecret,
      @Value("${app.solapi.sender:}") String sender,
      NotificationHistoryRepository histories) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.sender = sender;
    this.histories = histories;
  }

  public void sendReservationConfirmed(Reservation reservation) {
    String message = message(reservation);
    if (!isConfigured()) {
      histories.save(
          new NotificationHistory(reservation, "SMS", "SKIPPED", message, "SOLAPI 설정 미완료"));
      return;
    }
    try {
      DefaultMessageService service = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
      service.send(createMessage(reservation, message), null);
      histories.save(new NotificationHistory(reservation, "SMS", "SUCCESS", message, null));
    } catch (Exception e) {
      histories.save(
          new NotificationHistory(reservation, "SMS", "FAILED", message, e.getMessage()));
    }
  }

  private Message createMessage(Reservation reservation, String text) {
    Message message = new Message();
    message.setFrom(sender.replace("-", ""));
    message.setTo(reservation.getContactPhone().replace("-", ""));
    message.setText(text);
    return message;
  }

  private String message(Reservation reservation) {
    return "[다케어] 방문 수리 일정 확정\n"
        + "담당 기사: " + reservation.getEngineer().getName() + "\n"
        + "방문 일시: " + reservation.getConfirmedAt().format(DATE_TIME_FORMAT);
  }

  private boolean isConfigured() {
    return !apiKey.isBlank() && !apiSecret.isBlank() && !sender.isBlank();
  }
}
