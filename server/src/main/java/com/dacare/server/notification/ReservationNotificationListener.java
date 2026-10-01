package com.dacare.server.notification;

import com.dacare.server.repository.ReservationRepository;
import com.dacare.server.service.event.ReservationConfirmedEvent;
import com.dacare.server.service.event.ReservationReceivedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ReservationNotificationListener {

  private final ReservationRepository reservations;
  private final SlackNotificationSender slack;
  private final SolapiSmsNotificationSender sms;

  ReservationNotificationListener(ReservationRepository reservations,
      SlackNotificationSender slack, SolapiSmsNotificationSender sms) {
    this.reservations = reservations;
    this.slack = slack;
    this.sms = sms;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReceived(ReservationReceivedEvent event) {
    reservations.findById(event.reservationId()).ifPresent(slack::sendReservationReceived);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onConfirmed(ReservationConfirmedEvent event) {
    reservations.findById(event.reservationId()).ifPresent(sms::sendReservationConfirmed);
  }
}
