package com.dacare.server.service;

import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.error.BusinessException;
import com.dacare.server.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 비회원 예약 조회의 대입 시도를 막는다. 실패가 한도에 이르면 남은 시간 동안 해당 예약 번호, 휴대전화 번호 또는 IP의 조회를 거부한다.
 * <p>
 * 휴대전화 번호 기준 한도는 특정인의 번호를 알고 예약 번호를 대입하는 경우를, 예약 번호 기준 한도는 예약 번호를 알고 번호를 대입하는
 * 경우를, IP 기준 한도는 한 IP에서 여러 대상을 훑는 경우를 막는다. 단일 인스턴스 배포를 전제로 메모리에 보관하므로 서버를 재시작하면
 * 초기화된다.
 */
@Component
public class GuestLookupThrottle {

  public static final int MAX_FAILURES_PER_TARGET = 5;
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
  public void check(String code, PhoneNumber phone, String clientKey) {
    Instant now = clock.instant();
    if (reachedLimit(codeKey(code), MAX_FAILURES_PER_TARGET, now)
        || reachedLimit(phoneKey(phone), MAX_FAILURES_PER_TARGET, now)
        || reachedLimit(clientKey(clientKey), MAX_FAILURES_PER_CLIENT, now)) {
      throw new TooManyAttemptsException();
    }
  }

  public void recordFailure(String code, PhoneNumber phone, String clientKey) {
    Instant now = clock.instant();
    if (failures.size() > PRUNE_THRESHOLD) {
      failures.values().removeIf(entry -> entry.expired(now));
    }
    increment(codeKey(code), now);
    increment(phoneKey(phone), now);
    increment(clientKey(clientKey), now);
  }

  /**
   * 조회에 성공하면 그 예약과 휴대전화 번호의 실패 기록을 지운다. IP 기록은 여러 대상을 훑는 경우를 막기 위해 유지한다.
   */
  public void reset(String code, PhoneNumber phone) {
    failures.remove(codeKey(code));
    failures.remove(phoneKey(phone));
  }

  private boolean reachedLimit(String key, int limit, Instant now) {
    Failures entry = failures.get(key);
    return entry != null && !entry.expired(now) && entry.count() >= limit;
  }

  private void increment(String key, Instant now) {
    failures.compute(key, (ignored, entry) -> entry == null || entry.expired(now)
        ? new Failures(1, now) : new Failures(entry.count() + 1, entry.windowStart()));
  }

  private static String codeKey(String code) {
    return "code:" + code;
  }

  private static String phoneKey(PhoneNumber phone) {
    return "phone:" + phone.value();
  }

  private static String clientKey(String clientKey) {
    return "ip:" + clientKey;
  }

  private record Failures(int count, Instant windowStart) {

    boolean expired(Instant now) {
      return !now.isBefore(windowStart.plus(WINDOW));
    }
  }

  public static class TooManyAttemptsException extends BusinessException {

    public TooManyAttemptsException() {
      super(ErrorCode.TOO_MANY_ATTEMPTS);
    }
  }
}
