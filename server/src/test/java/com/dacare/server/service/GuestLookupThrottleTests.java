package com.dacare.server.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.service.GuestLookupThrottle.TooManyAttemptsException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * 예약 번호별·휴대전화 번호별·IP별 실패 한도, 시간 경과 후 해제, 성공 시 대상 기록 초기화를 검증한다.
 */
class GuestLookupThrottleTests {

  private static final PhoneNumber PHONE = PhoneNumber.of("010-1234-5678");
  private static final PhoneNumber OTHER_PHONE = PhoneNumber.of("010-8765-4321");

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
  void locksPhoneAfterGuessingManyCodes() {
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_TARGET; i++) {
      throttle.recordFailure("%08d".formatted(i), PHONE, "ip-" + i);
    }

    assertThatThrownBy(() -> throttle.check("99999999", PHONE, "new-ip"))
        .isInstanceOf(TooManyAttemptsException.class);
    assertThatCode(() -> throttle.check("99999999", OTHER_PHONE, "new-ip"))
        .doesNotThrowAnyException();
  }

  @Test
  void locksCodeAfterGuessingManyPhones() {
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_TARGET; i++) {
      throttle.recordFailure("11111111", PhoneNumber.of("010-0000-000" + i), "ip-" + i);
    }

    assertThatThrownBy(() -> throttle.check("11111111", PHONE, "new-ip"))
        .isInstanceOf(TooManyAttemptsException.class);
  }

  @Test
  void locksClientAfterScanningManyTargets() {
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_CLIENT; i++) {
      throttle.recordFailure("%08d".formatted(i), PhoneNumber.of("010-0000-%04d".formatted(i)),
          "ip");
    }

    assertThatThrownBy(() -> throttle.check("99999999", PHONE, "ip"))
        .isInstanceOf(TooManyAttemptsException.class);
    assertThatCode(() -> throttle.check("99999999", PHONE, "other-ip"))
        .doesNotThrowAnyException();
  }

  @Test
  void unlocksAfterWindow() {
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_TARGET; i++) {
      throttle.recordFailure("11111111", PHONE, "ip");
    }

    now.set(now.get().plus(GuestLookupThrottle.WINDOW));

    assertThatCode(() -> throttle.check("11111111", PHONE, "ip")).doesNotThrowAnyException();
  }

  @Test
  void successClearsTargetFailures() {
    for (int i = 0; i < GuestLookupThrottle.MAX_FAILURES_PER_TARGET - 1; i++) {
      throttle.recordFailure("11111111", PHONE, "ip-" + i);
    }
    throttle.reset("11111111", PHONE);
    throttle.recordFailure("11111111", PHONE, "ip-x");

    assertThatCode(() -> throttle.check("11111111", PHONE, "other-ip"))
        .doesNotThrowAnyException();
  }
}
