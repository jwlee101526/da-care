package com.dacare.server.service.event;

/**
 * 관리자가 기사와 방문 일시를 확정했다. 트랜잭션 커밋 후 고객 문자 알림에 사용한다.
 */
public record ReservationConfirmedEvent(Long reservationId) {

}
