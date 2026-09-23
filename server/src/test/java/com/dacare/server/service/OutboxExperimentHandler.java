package com.dacare.server.service;

import com.dacare.evidence.OutboxPrototype;
import org.springframework.context.event.EventListener;

/**
 * Outbox 장애 복구 실험에서 예약 접수 이벤트를 발송 요청으로 기록한다.
 * 운영 알림 처리기와 분리된 테스트 전용 핸들러다.
 */
public class OutboxExperimentHandler {

  private final OutboxPrototype outbox;

  public OutboxExperimentHandler(OutboxPrototype outbox) {
    this.outbox = outbox;
  }

  @EventListener
  public void handle(ReservationReceivedEvent event) {
    outbox.enqueue(event.reservationId());
  }
}
