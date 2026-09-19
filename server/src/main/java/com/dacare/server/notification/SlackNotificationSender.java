package com.dacare.server.notification;
import com.dacare.server.domain.NotificationHistory;
import com.dacare.server.domain.Reservation;
import com.dacare.server.repository.NotificationHistoryRepository;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class SlackNotificationSender {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E) HH:mm");

    private final String webhookUrl;
    private final NotificationHistoryRepository histories;
    private final RestClient client = RestClient.create();

    public SlackNotificationSender(
            @Value("${app.slack.webhook-url:}") String webhookUrl,
            NotificationHistoryRepository histories) {
        this.webhookUrl = webhookUrl;
        this.histories = histories;
    }

    public void sendReservationReceived(Reservation reservation) {
        String message = message(reservation);
        if (webhookUrl.isBlank()) {
            histories.save(new NotificationHistory(reservation, "SLACK", "SKIPPED", message, "SLACK_WEBHOOK_URL 미설정"));
            return;
        }

        try {
            client.post().uri(webhookUrl).body(Map.of("text", message)).retrieve().toBodilessEntity();
            histories.save(new NotificationHistory(reservation, "SLACK", "SUCCESS", message, null));
        } catch (RuntimeException e) {
            histories.save(new NotificationHistory(reservation, "SLACK", "FAILED", message, e.getMessage()));
        }
    }

    private String message(Reservation reservation) {
        return "🔔 *새 수리 예약이 접수되었습니다*\n"
                + "━━━━━━━━━━━━━━━━━━\n"
                + "*접수 번호*  #" + reservation.getId() + "\n"
                + "*진행 상태*  배정 대기\n\n"
                + "*고객 정보*\n"
                + "• 성함: " + reservation.getContactName() + "\n"
                + "• 연락처: " + reservation.getContactPhone() + "\n"
                + "• 방문 주소: " + reservation.getVisitAddress() + "\n\n"
                + "*수리 요청*\n"
                + "• 품목: " + deviceName(reservation.getDeviceType()) + "\n"
                + "• 증상: " + reservation.getSymptomDescription() + "\n"
                + "• 희망 방문: " + reservation.getPreferredAt().format(DATE_TIME_FORMAT) + "\n";
    }

    private String deviceName(String deviceType) {
        return switch (deviceType.toLowerCase()) {
            case "smartphone" -> "스마트폰 · 태블릿";
            case "computer", "laptop" -> "데스크탑 · PC / 노트북";
            case "tv" -> "스마트 TV";
            case "aircon" -> "에어컨";
            case "washing" -> "세탁기 · 건조기";
            case "fridge" -> "냉장고";
            case "microwave" -> "전자레인지 · 인덕션";
            case "cleaner" -> "청소기";
            case "console" -> "게임 콘솔";
            case "internet" -> "네트워크 · 공유기";
            case "audio" -> "음향 기기 · 오디오";
            case "repair" -> "긴급 출장 A/S";
            case "appliance" -> "생활 가전";
            case "etc" -> "기타 기기";
            default -> deviceType;
        };
    }
}
