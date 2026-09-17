package com.dacare.server.notification;
import com.dacare.server.domain.*;
import com.dacare.server.repository.NotificationHistoryRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class SlackNotificationSender implements NotificationSender {
    private final String webhookUrl; private final NotificationHistoryRepository histories; private final RestClient client = RestClient.create();
    public SlackNotificationSender(@Value("${app.slack.webhook-url:}") String webhookUrl, NotificationHistoryRepository histories) { this.webhookUrl = webhookUrl; this.histories = histories; }
    public String channel() { return "SLACK"; }
    public void send(Reservation reservation) { String message = "DA-Care 예약 확정 #" + reservation.getId() + " / " + reservation.getDeviceType() + " / " + reservation.getConfirmedAt(); if (webhookUrl.isBlank()) { histories.save(new NotificationHistory(reservation, channel(), "SKIPPED", message, "SLACK_WEBHOOK_URL 미설정")); return; } try { client.post().uri(webhookUrl).body(Map.of("text", message)).retrieve().toBodilessEntity(); histories.save(new NotificationHistory(reservation, channel(), "SUCCESS", message, null)); } catch (RuntimeException e) { histories.save(new NotificationHistory(reservation, channel(), "FAILED", message, e.getMessage())); } }
}
