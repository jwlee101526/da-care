package com.dacare.server.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 비회원 예약 조회의 비밀번호 대입을 막는다. 실패가 한도에 이르면 남은 시간 동안 해당 예약 번호 또는 IP의 조회를 거부한다.
 * <p>
 * 예약 번호 기준 한도는 여러 IP에서 한 예약을 노리는 경우를, IP 기준 한도는 한 IP에서 여러 예약을 훑는 경우를 막는다.
 * 단일 인스턴스 배포를 전제로 메모리에 보관하므로 서버를 재시작하면 초기화된다.
 */
@Component
public class GuestLookupThrottle {

  public static final int MAX_FAILURES_PER_RESERVATION = 5;
  static final int MAX_FAILURES_PER_CLIENT = 20;
  static final Duration WINDOW = Duration.ofMinutes(15);
  private static final int PRUNE_THRESHOLD = 10_000;

  private final Map<String, Failures> failures = new ConcurrentHashMap<>();
  private final Clock clock;

  public GuestLookupThrottle(Clock clock) {
    this.clock = clock;
  }

  /**
   * @param clientKey 요청 IP의 해시
   */
  public void check(Long reservationId, String clientKey) {
    Instant now = clock.instant();
    if (reachedLimit(reservationKey(reservationId), MAX_FAILURES_PER_RESERVATION, now)
        || reachedLimit(clientKey(clientKey), MAX_FAILURES_PER_CLIENT, now)) {
      throw new TooManyAttemptsException();
    }
  }

  public void recordFailure(Long reservationId, String clientKey) {
    Instant now = clock.instant();
    if (failures.size() > PRUNE_THRESHOLD) {
      failures.values().removeIf(entry -> entry.expired(now));
    }
    increment(reservationKey(reservationId), now);
    increment(clientKey(clientKey), now);
  }

  /**
   * 조회에 성공하면 그 예약의 실패 기록을 지운다. IP 기록은 다른 예약을 훑는 경우를 막기 위해 유지한다.
   */
  public void reset(Long reservationId) {
    failures.remove(reservationKey(reservationId));
  }

  private boolean reachedLimit(String key, int limit, Instant now) {
    Failures entry = failures.get(key);
    return entry != null && !entry.expired(now) && entry.count() >= limit;
  }

  private void increment(String key, Instant now) {
    failures.compute(key, (ignored, entry) -> entry == null || entry.expired(now)
        ? new Failures(1, now) : new Failures(entry.count() + 1, entry.windowStart()));
  }

  private static String reservationKey(Long reservationId) {
    return "reservation:" + reservationId;
  }

  private static String clientKey(String clientKey) {
    return "ip:" + clientKey;
  }

  private record Failures(int count, Instant windowStart) {

    boolean expired(Instant now) {
      return !now.isBefore(windowStart.plus(WINDOW));
    }
  }

  public static class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException() {
      super("조회 시도가 너무 많습니다. 15분 후 다시 시도해 주세요.");
    }
  }
}
