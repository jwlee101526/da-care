package com.dacare.server.notification;

import com.dacare.server.domain.NotificationHistory;
import com.dacare.server.domain.Reservation;
import com.dacare.server.repository.NotificationHistoryRepository;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.model.MessageType;
import com.solapi.sdk.message.model.kakao.KakaoOption;
import com.solapi.sdk.message.service.DefaultMessageService;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SolapiCustomerNotificationSender {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm");
    private final String apiKey;
    private final String apiSecret;
    private final String sender;
    private final String kakaoPfId;
    private final String kakaoTemplateId;
    private final NotificationHistoryRepository histories;

    public SolapiCustomerNotificationSender(
            @Value("${app.solapi.api-key:}") String apiKey,
            @Value("${app.solapi.api-secret:}") String apiSecret,
            @Value("${app.solapi.sender:}") String sender,
            @Value("${app.solapi.kakao-pf-id:}") String kakaoPfId,
            @Value("${app.solapi.kakao-template-id:}") String kakaoTemplateId,
            NotificationHistoryRepository histories) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.sender = sender;
        this.kakaoPfId = kakaoPfId;
        this.kakaoTemplateId = kakaoTemplateId;
        this.histories = histories;
    }

    public void sendReservationConfirmed(Reservation reservation) {
        String message = fallbackMessage(reservation);
        if (!isConfigured()) {
            histories.save(new NotificationHistory(reservation, "KAKAO_ALIMTALK", "SKIPPED", message, "SOLAPI 설정 미완료"));
            return;
        }
        try {
            DefaultMessageService service = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
            service.send(createMessage(reservation, message), null);
            histories.save(new NotificationHistory(reservation, "KAKAO_ALIMTALK", "SUCCESS", message, null));
        } catch (Exception e) {
            histories.save(new NotificationHistory(reservation, "KAKAO_ALIMTALK", "FAILED", message, e.getMessage()));
        }
    }

    private Message createMessage(Reservation reservation, String fallbackMessage) {
        KakaoOption kakao = new KakaoOption();
        kakao.setPfId(kakaoPfId);
        kakao.setTemplateId(kakaoTemplateId);
        kakao.setVariables(Map.of(
                "reservationId", String.valueOf(reservation.getId()),
                "engineerName", reservation.getEngineer().getName(),
                "confirmedAt", reservation.getConfirmedAt().format(DATE_TIME_FORMAT)));
        Message message = new Message();
        message.setType(MessageType.ATA);
        message.setFrom(sender.replace("-", ""));
        message.setTo(reservation.getContactPhone().replace("-", ""));
        message.setText(fallbackMessage);
        message.setKakaoOptions(kakao);
        return message;
    }

    private String fallbackMessage(Reservation reservation) {
        return "[DA-Care] 예약 #" + reservation.getId() + "이 확정되었습니다. 담당 기사: "
                + reservation.getEngineer().getName() + ", 방문 일시: " + reservation.getConfirmedAt().format(DATE_TIME_FORMAT);
    }

    private boolean isConfigured() {
        return !apiKey.isBlank() && !apiSecret.isBlank() && !sender.isBlank() && !kakaoPfId.isBlank() && !kakaoTemplateId.isBlank();
    }
}
