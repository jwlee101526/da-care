package com.dacare.server.service.event;

/**
 * 예약이 새로 접수되었다. 트랜잭션 커밋 후 운영 채널 알림에 사용한다.
 */
public record ReservationReceivedEvent(Long reservationId) {

}
