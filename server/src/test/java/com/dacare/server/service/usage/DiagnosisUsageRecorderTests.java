package com.dacare.server.service.usage;

import static org.assertj.core.api.Assertions.assertThat;

import com.dacare.server.domain.DiagnosisOutcome;
import com.dacare.server.domain.DiagnosisUsageLog;
import com.dacare.server.repository.DiagnosisUsageLogRepository;
import com.dacare.server.repository.DiagnosisUsageSummary;
import com.dacare.server.service.diagnosis.DiagnosisMeter.Snapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * H2에 상담 사용량을 저장해 한국 시간 주 기준 집계와 interrupt 상태에서의 기록을 검증한다.
 */
@SpringBootTest
class DiagnosisUsageRecorderTests {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final UsageSubject GUEST = UsageSubject.guest("ip-hash");

  @Autowired
  private DiagnosisUsageLogRepository repository;
  private DiagnosisUsageRecorder recorder;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    // 월요일 00:10(한국 시간). UTC로는 아직 지난주 일요일이다.
    Clock clock = Clock.fixed(ZonedDateTime.of(2026, 10, 5, 0, 10, 0, 0, SEOUL).toInstant(), SEOUL);
    recorder = new DiagnosisUsageRecorder(repository, clock);
  }

  @Test
  void summarizesCurrentWeekOnly() {
    recorder.record(GUEST, DiagnosisOutcome.COMPLETED,
        new Snapshot("gpt-5-mini", 3, 2, 3000, 2048, 400, 128), Duration.ofMillis(4000));
    recorder.record(UsageSubject.customer("demo@dacare.com"), DiagnosisOutcome.TIMEOUT,
        new Snapshot("gpt-5-mini", 1, 0, 1000, 0, 100, 0), Duration.ofMillis(8000));
    repository.save(new DiagnosisUsageLog("GUEST", DiagnosisOutcome.FAILED, null, 9, 9, 9, 9, 9,
        9, 9, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 28).atStartOfDay()));

    DiagnosisUsageSummary summary = recorder.currentWeek();

    assertThat(summary).isEqualTo(
        new DiagnosisUsageSummary(2, 1, 0, 1, 0, 6000.0, 2.0, 4000, 2048, 500, 128));
    assertThat(repository.findAll()).filteredOn(log -> log.getPeriodStart()
            .equals(LocalDate.of(2026, 10, 5)))
        .extracting(DiagnosisUsageLog::getSubjectType).containsExactlyInAnyOrder("GUEST", "CUSTOMER");
  }

  @Test
  void emptyWeekIsAllZero() {
    assertThat(recorder.currentWeek())
        .isEqualTo(new DiagnosisUsageSummary(0, 0, 0, 0, 0, 0.0, 0.0, 0, 0, 0, 0));
  }

  @Test
  void cancelledThreadStillRecordsAndKeepsInterruptFlag() {
    Thread.currentThread().interrupt();
    try {
      recorder.record(GUEST, DiagnosisOutcome.CANCELLED,
          new Snapshot(null, 1, 0, 500, 0, 0, 0), Duration.ofMillis(1500));
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
    assertThat(repository.count()).isEqualTo(1);
  }
}
