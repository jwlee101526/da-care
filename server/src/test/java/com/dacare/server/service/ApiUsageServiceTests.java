package com.dacare.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dacare.server.domain.PaidApi;
import com.dacare.server.repository.ApiUsageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * H2에 실제 카운터를 저장해 하루 한도, 한국 시간 자정 초기화, 반환 동작을 검증한다.
 */
@SpringBootTest
class ApiUsageServiceTests {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  @Autowired
  private ApiUsageRepository repository;
  private final AtomicReference<Instant> now = new AtomicReference<>();
  private ApiUsageService service;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    now.set(ZonedDateTime.of(2026, 9, 30, 23, 50, 0, 0, SEOUL).toInstant());
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
    service = new ApiUsageService(repository, clock, 2, 1);
  }

  @Test
  void acquiresUntilDailyLimit() {
    assertThat(service.tryAcquire(PaidApi.DIAGNOSIS)).isTrue();
    assertThat(service.tryAcquire(PaidApi.DIAGNOSIS)).isTrue();
    assertThat(service.tryAcquire(PaidApi.DIAGNOSIS)).isFalse();

    ApiUsageService.DailyUsage usage = service.todayUsage();
    assertThat(usage.diagnosis()).isEqualTo(new ApiUsageService.Usage(2, 2, 0));
    assertThat(usage.sms()).isEqualTo(new ApiUsageService.Usage(0, 1, 1));
  }

  @Test
  void limitsAreCountedPerApi() {
    assertThat(service.tryAcquire(PaidApi.SMS)).isTrue();
    assertThat(service.tryAcquire(PaidApi.SMS)).isFalse();
    assertThat(service.tryAcquire(PaidApi.DIAGNOSIS)).isTrue();
  }

  @Test
  void resetsAtSeoulMidnight() {
    assertThat(service.tryAcquire(PaidApi.SMS)).isTrue();
    assertThat(service.tryAcquire(PaidApi.SMS)).isFalse();

    now.set(now.get().plusSeconds(15 * 60));

    assertThat(service.todayUsage().sms().used()).isZero();
    assertThat(service.tryAcquire(PaidApi.SMS)).isTrue();
  }

  @Test
  void releaseReturnsOneCall() {
    assertThat(service.tryAcquire(PaidApi.SMS)).isTrue();
    service.release(PaidApi.SMS);

    assertThat(service.todayUsage().sms().used()).isZero();
    assertThat(service.tryAcquire(PaidApi.SMS)).isTrue();
  }
}
