package com.dacare.server.service;

import com.dacare.server.domain.ApiUsage;
import com.dacare.server.domain.PaidApi;
import com.dacare.server.repository.ApiUsageRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유료 API의 주간 호출 한도를 관리한다. 주는 주입된 Clock의 시간대(Asia/Seoul) 기준 월요일 0시에 시작한다.
 * <p>
 * AI 상담은 사용자별 한도와 서비스 전체 한도를 모두 통과해야 한다. SMS는 서비스 전체 한도만 둔다.
 */
@Service
public class ApiUsageService {

  private static final String TOTAL = "total";

  private final ApiUsageRepository usages;
  private final Clock clock;
  private final UsageLimits limits;

  public ApiUsageService(ApiUsageRepository usages, Clock clock, UsageLimits limits) {
    this.usages = usages;
    this.clock = clock;
    this.limits = limits;
  }

  /**
   * 사용자 한도를 먼저 차감하고, 서비스 전체 한도가 소진됐으면 사용자 차감분을 되돌린다.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public AcquireResult tryAcquireDiagnosis(UsageSubject subject) {
    LocalDate period = currentPeriod();
    if (!acquire(PaidApi.DIAGNOSIS, subject.storageKey(), period, limits.diagnosis(subject))) {
      return AcquireResult.SUBJECT_LIMIT_REACHED;
    }
    if (!acquire(PaidApi.DIAGNOSIS, TOTAL, period, limits.diagnosisTotal())) {
      usages.decrement(PaidApi.DIAGNOSIS, subject.storageKey(), period);
      return AcquireResult.TOTAL_LIMIT_REACHED;
    }
    return AcquireResult.ACQUIRED;
  }

  /**
   * 차감했지만 처리하지 못한 상담 1회를 사용자와 서비스 전체 집계에서 되돌린다.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void releaseDiagnosis(UsageSubject subject) {
    LocalDate period = currentPeriod();
    usages.decrement(PaidApi.DIAGNOSIS, subject.storageKey(), period);
    usages.decrement(PaidApi.DIAGNOSIS, TOTAL, period);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public boolean tryAcquireSms() {
    return acquire(PaidApi.SMS, TOTAL, currentPeriod(), limits.sms());
  }

  /**
   * 사용자 기준 이번 주 상담 사용량. 남은 횟수는 서비스 전체 잔여량을 넘지 않는다.
   */
  @Transactional(readOnly = true)
  public SubjectUsage diagnosisUsage(UsageSubject subject) {
    LocalDate period = currentPeriod();
    int limit = limits.diagnosis(subject);
    int used = used(PaidApi.DIAGNOSIS, subject.storageKey(), period);
    int totalRemaining = remaining(limits.diagnosisTotal(),
        used(PaidApi.DIAGNOSIS, TOTAL, period));
    return new SubjectUsage(period, resetsAt(period),
        new Usage(used, limit, Math.min(remaining(limit, used), totalRemaining)));
  }

  /**
   * 서비스 전체 이번 주 사용량(관리자 화면용).
   */
  @Transactional(readOnly = true)
  public TotalUsage totalUsage() {
    LocalDate period = currentPeriod();
    return new TotalUsage(period, resetsAt(period),
        usage(PaidApi.DIAGNOSIS, period, limits.diagnosisTotal()),
        usage(PaidApi.SMS, period, limits.sms()));
  }

  private boolean acquire(PaidApi api, String subject, LocalDate period, int limit) {
    if (limit <= 0) {
      return false;
    }
    if (usages.increment(api, subject, period, limit) == 1) {
      return true;
    }
    if (usages.findByApiAndSubjectAndPeriodStart(api, subject, period).isPresent()) {
      return false;
    }
    try {
      usages.saveAndFlush(new ApiUsage(api, subject, period, 1));
      return true;
    } catch (DataIntegrityViolationException exception) {
      // 다른 요청이 먼저 이번 주 행을 만들었다.
      return usages.increment(api, subject, period, limit) == 1;
    }
  }

  private Usage usage(PaidApi api, LocalDate period, int limit) {
    int used = used(api, TOTAL, period);
    return new Usage(used, limit, remaining(limit, used));
  }

  private int used(PaidApi api, String subject, LocalDate period) {
    return usages.findByApiAndSubjectAndPeriodStart(api, subject, period)
        .map(ApiUsage::getUsed).orElse(0);
  }

  private static int remaining(int limit, int used) {
    return Math.max(0, limit - used);
  }

  private LocalDate currentPeriod() {
    return LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }

  private static LocalDateTime resetsAt(LocalDate period) {
    return period.plusWeeks(1).atStartOfDay();
  }

  public enum AcquireResult {ACQUIRED, SUBJECT_LIMIT_REACHED, TOTAL_LIMIT_REACHED}

  public record Usage(int used, int limit, int remaining) {

  }

  public record SubjectUsage(LocalDate periodStart, LocalDateTime resetsAt, Usage diagnosis) {

  }

  public record TotalUsage(LocalDate periodStart, LocalDateTime resetsAt, Usage diagnosis,
                           Usage sms) {

  }
}
