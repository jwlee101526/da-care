package com.dacare.server.service.usage;

import com.dacare.server.domain.DiagnosisOutcome;
import com.dacare.server.domain.DiagnosisUsageLog;
import com.dacare.server.repository.DiagnosisUsageLogRepository;
import com.dacare.server.repository.DiagnosisUsageSummary;
import com.dacare.server.service.diagnosis.DiagnosisMeter;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 상담 한 건의 모델 사용량을 남기고 주간 집계를 조회한다. 기록 실패는 상담 결과에 영향을 주지 않는다.
 */
@Service
public class DiagnosisUsageRecorder {

  private static final Logger log = LoggerFactory.getLogger(DiagnosisUsageRecorder.class);

  private final DiagnosisUsageLogRepository logs;
  private final Clock clock;

  public DiagnosisUsageRecorder(DiagnosisUsageLogRepository logs, Clock clock) {
    this.logs = logs;
    this.clock = clock;
  }

  public void record(UsageSubject subject, DiagnosisOutcome outcome, DiagnosisMeter.Snapshot usage,
      Duration duration) {
    // 취소된 상담 스레드는 interrupt 상태라 커넥션 획득이 실패할 수 있다. 저장하는 동안만 해제했다가 되돌린다.
    boolean interrupted = Thread.interrupted();
    try {
      logs.save(new DiagnosisUsageLog(subject.type().name(), outcome, usage.model(),
          usage.modelCalls(), usage.toolCalls(), duration.toMillis(), usage.inputTokens(),
          usage.cachedInputTokens(), usage.outputTokens(), usage.reasoningTokens(),
          ApiUsageService.weekStart(clock), LocalDateTime.now(clock)));
    } catch (RuntimeException exception) {
      log.warn("상담 사용량 기록 실패: {}", exception.getMessage());
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * 이번 주(한도와 같은 주 기준) 상담 사용량 집계(관리자 화면용).
   */
  @Transactional(readOnly = true)
  public DiagnosisUsageSummary currentWeek() {
    return logs.summarize(ApiUsageService.weekStart(clock));
  }
}
