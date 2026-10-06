package com.dacare.server.notification;

import static com.dacare.server.domain.NotificationChannel.SMS;
import static com.dacare.server.domain.NotificationStatus.FAILED;
import static com.dacare.server.domain.NotificationStatus.SKIPPED;
import static com.dacare.server.domain.NotificationStatus.SUCCESS;

import com.dacare.server.domain.NotificationHistory;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.domain.Reservation;
import com.dacare.server.repository.NotificationHistoryRepository;
import com.dacare.server.service.ApiUsageService;
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
  private final ApiUsageService usage;

  public SolapiSmsNotificationSender(
      @Value("${app.solapi.api-key:}") String apiKey,
      @Value("${app.solapi.api-secret:}") String apiSecret,
      @Value("${app.solapi.sender:}") String sender,
      NotificationHistoryRepository histories, ApiUsageService usage) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.sender = sender;
    this.histories = histories;
    this.usage = usage;
  }

  public void sendReservationConfirmed(Reservation reservation) {
    String message = message(reservation);
    if (!isConfigured()) {
      histories.save(
          new NotificationHistory(reservation, SMS, SKIPPED, message, "SOLAPI 설정 미완료"));
      return;
    }
    if (!usage.tryAcquireSms()) {
      histories.save(
          new NotificationHistory(reservation, SMS, SKIPPED, message, "주간 SMS 한도 초과"));
      return;
    }
    try {
      DefaultMessageService service = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
      service.send(createMessage(reservation, message), null);
      histories.save(new NotificationHistory(reservation, SMS, SUCCESS, message, null));
    } catch (Exception e) {
      histories.save(
          new NotificationHistory(reservation, SMS, FAILED, message, e.getMessage()));
    }
  }

  private Message createMessage(Reservation reservation, String text) {
    Message message = new Message();
    // 발신번호는 솔라피에 등록한 국내 번호만 쓸 수 있다.
    message.setFrom(PhoneNumber.of(sender).nationalDialingNumber());
    setRecipient(message, reservation.getContactPhone());
    message.setText(text);
    return message;
  }

  /**
   * 솔라피는 수신번호를 국가번호와 분리해 받는다. 국내 번호는 국내 형식(01012345678)으로, 해외 번호는 국가번호와 국내 접두어를 뺀 번호로 보낸다.
   */
  private void setRecipient(Message message, PhoneNumber to) {
    if (to.isDomestic()) {
      message.setTo(to.nationalDialingNumber());
      return;
    }
    message.setCountry(String.valueOf(to.countryCode()));
    message.setTo(to.nationalSignificantNumber());
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
