package com.dacare.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dacare.server.repository.ApiUsageRepository;
import com.dacare.server.service.ApiUsageService.AcquireResult;
import com.dacare.server.service.ApiUsageService.Usage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * H2에 실제 카운터를 저장해 사용자·서비스 전체 주간 한도, 한국 시간 월요일 0시 초기화, 반환 동작을 검증한다.
 */
@SpringBootTest
class ApiUsageServiceTests {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final UsageSubject CUSTOMER = UsageSubject.customer("demo@dacare.com");
  private static final UsageSubject OTHER_CUSTOMER = UsageSubject.customer("other@dacare.com");
  private static final UsageSubject ADMIN = UsageSubject.admin("admin@dacare.com");
  private static final UsageSubject GUEST = UsageSubject.guest("ip-hash");

  @Autowired
  private ApiUsageRepository repository;
  private final AtomicReference<Instant> now = new AtomicReference<>();
  private ApiUsageService service;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    // 일요일 23:50(한국 시간). 10분 뒤 새 주가 시작된다.
    now.set(ZonedDateTime.of(2026, 10, 4, 23, 50, 0, 0, SEOUL).toInstant());
    Clock clock = new Clock() {
      @Override
      public ZoneId getZone() {
        return SEOUL;
      }

      @Override
      public Clock withZone(ZoneId zone) {
        return this;
      }

      @Override
      public Instant instant() {
        return now.get();
      }
    };
    // 전체 4, 일반 2, 관리자 3, 비로그인 1, SMS 1
    service = new ApiUsageService(repository, clock, new UsageLimits(4, 2, 3, 1, 1));
  }

  @Test
  void eachSubjectHasItsOwnLimitByRole() {
    assertThat(service.tryAcquireDiagnosis(GUEST)).isEqualTo(AcquireResult.ACQUIRED);
    assertThat(service.tryAcquireDiagnosis(GUEST)).isEqualTo(AcquireResult.SUBJECT_LIMIT_REACHED);

    assertThat(service.tryAcquireDiagnosis(CUSTOMER)).isEqualTo(AcquireResult.ACQUIRED);
    assertThat(service.tryAcquireDiagnosis(CUSTOMER)).isEqualTo(AcquireResult.ACQUIRED);
    assertThat(service.tryAcquireDiagnosis(CUSTOMER)).isEqualTo(AcquireResult.SUBJECT_LIMIT_REACHED);

    ApiUsageService.SubjectUsage usage = service.diagnosisUsage(CUSTOMER);
    assertThat(usage.diagnosis()).isEqualTo(new Usage(2, 2, 0));
    assertThat(usage.periodStart()).isEqualTo(LocalDate.of(2026, 9, 28));
    assertThat(usage.resetsAt()).isEqualTo(LocalDate.of(2026, 10, 5).atStartOfDay());
  }

  @Test
  void totalLimitBlocksEveryoneWithoutChargingSubject() {
    service.tryAcquireDiagnosis(CUSTOMER);
    service.tryAcquireDiagnosis(CUSTOMER);
    service.tryAcquireDiagnosis(OTHER_CUSTOMER);
    service.tryAcquireDiagnosis(GUEST);

    assertThat(service.tryAcquireDiagnosis(ADMIN)).isEqualTo(AcquireResult.TOTAL_LIMIT_REACHED);
    // 전체 한도에 막힌 요청은 관리자 개인 사용량에 남지 않는다.
    assertThat(service.diagnosisUsage(ADMIN).diagnosis()).isEqualTo(new Usage(0, 3, 0));
    assertThat(service.totalUsage().diagnosis()).isEqualTo(new Usage(4, 4, 0));
  }

  @Test
  void remainingNeverExceedsTotalRemaining() {
    service.tryAcquireDiagnosis(CUSTOMER);
    service.tryAcquireDiagnosis(CUSTOMER);
    service.tryAcquireDiagnosis(GUEST);

    assertThat(service.diagnosisUsage(ADMIN).diagnosis()).isEqualTo(new Usage(0, 3, 1));
  }

  @Test
  void resetsAtSeoulMondayMidnight() {
    assertThat(service.tryAcquireDiagnosis(GUEST)).isEqualTo(AcquireResult.ACQUIRED);
    assertThat(service.tryAcquireSms()).isTrue();
    assertThat(service.tryAcquireSms()).isFalse();

    now.set(now.get().plusSeconds(15 * 60));

    assertThat(service.diagnosisUsage(GUEST).diagnosis().used()).isZero();
    assertThat(service.totalUsage().sms().used()).isZero();
    assertThat(service.tryAcquireDiagnosis(GUEST)).isEqualTo(AcquireResult.ACQUIRED);
    assertThat(service.tryAcquireSms()).isTrue();
  }

  @Test
  void releaseReturnsSubjectAndTotalCount() {
    assertThat(service.tryAcquireDiagnosis(GUEST)).isEqualTo(AcquireResult.ACQUIRED);
    service.releaseDiagnosis(GUEST);

    assertThat(service.diagnosisUsage(GUEST).diagnosis().used()).isZero();
    assertThat(service.totalUsage().diagnosis().used()).isZero();
    assertThat(service.tryAcquireDiagnosis(GUEST)).isEqualTo(AcquireResult.ACQUIRED);
  }
}
