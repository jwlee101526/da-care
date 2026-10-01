package com.dacare.server.service;

import com.dacare.server.domain.ApiUsage;
import com.dacare.server.domain.PaidApi;
import com.dacare.server.repository.ApiUsageRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유료 API의 서비스 전체 하루 호출 한도를 관리한다. 일자는 주입된 Clock의 시간대(Asia/Seoul) 기준이다.
 */
@Service
public class ApiUsageService {

  private final ApiUsageRepository usages;
  private final Clock clock;
  private final int diagnosisLimit;
  private final int smsLimit;

  public ApiUsageService(ApiUsageRepository usages, Clock clock,
      @Value("${app.usage.daily-limit.diagnosis}") int diagnosisLimit,
      @Value("${app.usage.daily-limit.sms}") int smsLimit) {
    this.usages = usages;
    this.clock = clock;
    this.diagnosisLimit = diagnosisLimit;
    this.smsLimit = smsLimit;
  }

  /**
   * 오늘 한도가 남아 있으면 1회를 사용하고 true를 반환한다.
   * <p>
   * 호출자의 트랜잭션과 분리해, 첫 호출이 동시에 들어와 일자 행 생성이 충돌해도 호출자 트랜잭션이 롤백되지 않게 한다.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public boolean tryAcquire(PaidApi api) {
    int limit = limit(api);
    LocalDate today = today();
    if (limit <= 0) {
      return false;
    }
    if (usages.increment(api, today, limit) == 1) {
      return true;
    }
    if (usages.findByApiAndUsageDate(api, today).isPresent()) {
      return false;
    }
    try {
      usages.saveAndFlush(new ApiUsage(api, today, 1));
      return true;
    } catch (DataIntegrityViolationException exception) {
      // 다른 요청이 먼저 오늘 행을 만들었다.
      return usages.increment(api, today, limit) == 1;
    }
  }

  /**
   * 사용했지만 처리하지 못한 요청의 1회를 되돌린다.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void release(PaidApi api) {
    usages.decrement(api, today());
  }

  @Transactional(readOnly = true)
  public DailyUsage todayUsage() {
    LocalDate today = today();
    return new DailyUsage(today, usage(PaidApi.DIAGNOSIS, today), usage(PaidApi.SMS, today));
  }

  private Usage usage(PaidApi api, LocalDate date) {
    int used = usages.findByApiAndUsageDate(api, date).map(ApiUsage::getUsed).orElse(0);
    int limit = limit(api);
    return new Usage(used, limit, Math.max(0, limit - used));
  }

  private int limit(PaidApi api) {
    return switch (api) {
      case DIAGNOSIS -> diagnosisLimit;
      case SMS -> smsLimit;
    };
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  public record Usage(int used, int limit, int remaining) {

  }

  public record DailyUsage(LocalDate date, Usage diagnosis, Usage sms) {

  }
}
