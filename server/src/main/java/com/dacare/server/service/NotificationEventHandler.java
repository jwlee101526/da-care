package com.dacare.server.service;
import com.dacare.server.notification.NotificationSender;
import com.dacare.server.repository.ReservationRepository;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
class NotificationEventHandler {
    private final ReservationRepository reservations; private final List<NotificationSender> senders;
    NotificationEventHandler(ReservationRepository reservations, List<NotificationSender> senders) { this.reservations = reservations; this.senders = senders; }
    @Async @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) public void handle(ReservationConfirmedEvent event) { var reservation = reservations.findById(event.reservationId()).orElse(null); if (reservation != null) senders.forEach(sender -> sender.send(reservation)); }
}
