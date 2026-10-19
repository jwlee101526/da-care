package com.dacare.server.notification;

import com.dacare.server.repository.ReservationRepository;
import com.dacare.server.service.event.ReservationConfirmedEvent;
import com.dacare.server.service.event.ReservationReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 예약 커밋 후 알림을 보낸다. 커밋 후 리스너의 예외는 예약 요청의 응답으로 전파되므로, 이미 저장된 예약이 실패로 보이지 않도록 알림 처리 실패는 기록만 한다.
 * <p>
 * 알림 이력은 새 트랜잭션에 저장한다. 그 안에서 난 예외로 rollback-only가 된 트랜잭션은 커밋 시점에 다시 예외를 던지므로, 예외는 트랜잭션 경계 바깥에서 잡는다.
 */
@Component
class ReservationNotificationListener {

  private static final Logger log = LoggerFactory.getLogger(ReservationNotificationListener.class);

  private final ReservationRepository reservations;
  private final SlackNotificationSender slack;
  private final SolapiSmsNotificationSender sms;
  private final TransactionTemplate newTransaction;

  ReservationNotificationListener(ReservationRepository reservations,
      SlackNotificationSender slack, SolapiSmsNotificationSender sms,
      PlatformTransactionManager transactionManager) {
    this.reservations = reservations;
    this.slack = slack;
    this.sms = sms;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReceived(ReservationReceivedEvent event) {
    try {
      newTransaction.executeWithoutResult(status -> reservations.findById(event.reservationId())
          .ifPresent(slack::sendReservationReceived));
    } catch (RuntimeException exception) {
      log.error("예약 접수 알림 처리 실패: reservation={}", event.reservationId(), exception);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onConfirmed(ReservationConfirmedEvent event) {
    try {
      newTransaction.executeWithoutResult(status -> reservations.findById(event.reservationId())
          .ifPresent(sms::sendReservationConfirmed));
    } catch (RuntimeException exception) {
      log.error("예약 확정 알림 처리 실패: reservation={}", event.reservationId(), exception);
    }
  }
}
