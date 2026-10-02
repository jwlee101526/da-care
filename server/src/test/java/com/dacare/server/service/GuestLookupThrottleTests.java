package com.dacare.server.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dacare.server.service.GuestLookupThrottle.TooManyAttemptsException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * 예약 번호별·IP별 실패 한도, 시간 경과 후 해제, 성공 시 예약 기록 초기화를 검증한다.
 */
class GuestLookupThrottleTests {

  private final AtomicReference<Instant> now = new AtomicReference<>(
      Instant.parse("2026-10-02T00:00:00Z"));
  private final GuestLookupThrottle throttle = new GuestLookupThrottle(new Clock() {
    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now.get();
    }
  });

  @Test
  void locksReservationAfterRepeatedFailuresFromDifferentClients() {
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_RESERVATION; i++) {
      throttle.recordFailure(1L, "ip-" + i);
    }

    assertThatThrownBy(() -> throttle.check(1L, "new-ip"))
        .isInstanceOf(TooManyAttemptsException.class);
    assertThatCode(() -> throttle.check(2L, "new-ip")).doesNotThrowAnyException();
  }

  @Test
  void locksClientAfterScanningManyReservations() {
    for (long id = 1; id <= GuestLookupThrottle.MAX_FAILURES_PER_CLIENT; id++) {
      throttle.recordFailure(id, "ip");
    }

    assertThatThrownBy(() -> throttle.check(999L, "ip"))
        .isInstanceOf(TooManyAttemptsException.class);
    assertThatCode(() -> throttle.check(999L, "other-ip")).doesNotThrowAnyException();
  }

  @Test
  void unlocksAfterWindow() {
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_RESERVATION; i++) {
      throttle.recordFailure(1L, "ip");
    }

    now.set(now.get().plus(GuestLookupThrottle.WINDOW));

    assertThatCode(() -> throttle.check(1L, "ip")).doesNotThrowAnyException();
  }

  @Test
  void successClearsReservationFailures() {
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_RESERVATION - 1; i++) {
      throttle.recordFailure(1L, "ip");
    }
    throttle.reset(1L);
    throttle.recordFailure(1L, "ip");

    assertThatCode(() -> throttle.check(1L, "other-ip")).doesNotThrowAnyException();
  }
}
