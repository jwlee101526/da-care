package com.dacare.server.service;
import com.dacare.server.notification.SlackNotificationSender;
import com.dacare.server.notification.SolapiSmsNotificationSender;
import com.dacare.server.repository.ReservationRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
class NotificationEventHandler {
    private final ReservationRepository reservations; private final SlackNotificationSender slack; private final SolapiSmsNotificationSender customers;
    NotificationEventHandler(ReservationRepository reservations, SlackNotificationSender slack, SolapiSmsNotificationSender customers) { this.reservations = reservations; this.slack = slack; this.customers = customers; }
    @Transactional(propagation = Propagation.REQUIRES_NEW) @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) public void handle(ReservationReceivedEvent event) { reservations.findById(event.reservationId()).ifPresent(slack::sendReservationReceived); }
    @Transactional(propagation = Propagation.REQUIRES_NEW) @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) public void handle(ReservationConfirmedEvent event) { reservations.findById(event.reservationId()).ifPresent(customers::sendReservationConfirmed); }
}
