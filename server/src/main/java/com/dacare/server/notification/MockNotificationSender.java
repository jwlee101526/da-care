package com.dacare.server.notification;
import com.dacare.server.domain.*;
import com.dacare.server.repository.NotificationHistoryRepository;
import org.slf4j.*;
import org.springframework.stereotype.Component;
@Component
public class MockNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(MockNotificationSender.class); private final NotificationHistoryRepository histories;
    public MockNotificationSender(NotificationHistoryRepository histories) { this.histories = histories; }
    public String channel() { return "MOCK_CUSTOMER"; }
    public void send(Reservation reservation) { String message = "[예약확정] " + reservation.getConfirmedAt() + " 방문 예정입니다."; histories.save(new NotificationHistory(reservation, channel(), "SUCCESS", message, null)); log.info("Mock customer notification: reservationId={}, message={}", reservation.getId(), message); }
}
