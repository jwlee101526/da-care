package com.dacare.server.service.diagnosis;

import com.dacare.server.domain.DiagnosisOutcome;
import com.dacare.server.error.ErrorCode;
import com.dacare.server.service.diagnosis.DiagnosisService.ConversationTurn;
import com.dacare.server.service.usage.ApiUsageService.AcquireResult;
import com.dacare.server.service.usage.ApiUsageService;
import com.dacare.server.service.usage.DiagnosisUsageRecorder;
import com.dacare.server.service.usage.UsageSubject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 상담 한 건의 실행 흐름. 모델을 쓸 수 있을 때만 주간 한도를 차감한 뒤 전용 스레드 풀에서 상담을 실행하고, 서버 기한이 지나면 작업을 중단한다.
 */
@Service
public class DiagnosisSessionService {

  private final DiagnosisService service;
  private final DiagnosisExecutor executor;
  private final ApiUsageService usage;
  private final DiagnosisUsageRecorder recorder;
  private final Duration timeout;

  public DiagnosisSessionService(DiagnosisService service, DiagnosisExecutor executor,
      ApiUsageService usage, DiagnosisUsageRecorder recorder,
      @Value("${app.diagnosis.timeout}") Duration timeout) {
    this.service = service;
    this.executor = executor;
    this.usage = usage;
    this.recorder = recorder;
    this.timeout = timeout;
  }

  public Duration timeout() {
    return timeout;
  }

  public DiagnosisSession start(String question, List<ConversationTurn> history,
      UsageSubject subject, String email, DiagnosisListener listener) {
    if (!service.isAvailable()) {
      listener.failed(ErrorCode.DIAGNOSIS_UNAVAILABLE);
      return DiagnosisSession.finishedSession();
    }
    AcquireResult acquired = usage.tryAcquireDiagnosis(subject);
    if (acquired != AcquireResult.ACQUIRED) {
      listener.failed(limitError(acquired), limitMessage(acquired, subject));
      return DiagnosisSession.finishedSession();
    }
    DiagnosisSession session = new DiagnosisSession();
    long startedAt = System.nanoTime();
    AtomicBoolean timedOut = new AtomicBoolean();
    try {
      session.start(
          executor.submit(() -> run(session, question, history, subject, email, listener,
              new Measurement(startedAt, timedOut))),
          executor.schedule(() -> {
            timedOut.set(true);
            // 작업 중단보다 안내를 먼저 보낸다. 중단된 작업이 먼저 종료를 알리면 안내가 유실된다.
            listener.failed(ErrorCode.DIAGNOSIS_TIMEOUT);
            session.cancel();
          }, timeout));
    } catch (RejectedExecutionException exception) {
      usage.releaseDiagnosis(subject);
      session.finish();
      listener.failed(ErrorCode.DIAGNOSIS_BUSY);
    }
    return session;
  }

  private void run(DiagnosisSession session, String question, List<ConversationTurn> history,
      UsageSubject subject, String email, DiagnosisListener listener, Measurement measurement) {
    DiagnosisMeter meter = new DiagnosisMeter();
    DiagnosisOutcome outcome = DiagnosisOutcome.FAILED;
    try {
      listener.completed(service.diagnose(question, history, email, listener::progress, meter));
      outcome = DiagnosisOutcome.COMPLETED;
    } catch (DiagnosisCancelledException exception) {
      outcome = DiagnosisOutcome.CANCELLED;
      listener.cancelled();
    } catch (DiagnosisUnavailableException exception) {
      listener.failed(exception.errorCode(), exception.getMessage());
    } catch (RuntimeException exception) {
      listener.failed(ErrorCode.DIAGNOSIS_FAILED);
    } finally {
      session.finish();
      // 기한 안내가 나갔으면 작업이 어떻게 끝났든 사용자에게는 시간 초과다.
      recorder.record(subject, measurement.timedOut().get() ? DiagnosisOutcome.TIMEOUT : outcome,
          meter.snapshot(), Duration.ofNanos(System.nanoTime() - measurement.startedAt()));
    }
  }

  /**
   * 사용량 기록용 측정값. 소요 시간은 대기열에서 기다린 시간을 포함한다.
   */
  private record Measurement(long startedAt, AtomicBoolean timedOut) {

  }

  private static ErrorCode limitError(AcquireResult result) {
    return result == AcquireResult.TOTAL_LIMIT_REACHED ? ErrorCode.SERVICE_LIMIT_EXCEEDED
        : ErrorCode.WEEKLY_LIMIT_EXCEEDED;
  }

  private static String limitMessage(AcquireResult result, UsageSubject subject) {
    if (result == AcquireResult.SUBJECT_LIMIT_REACHED && subject.isGuest()) {
      return "이번 주 비로그인 상담 가능 횟수를 모두 사용했습니다. 로그인하면 상담을 계속 이용할 수 있습니다.";
    }
    return limitError(result).message();
  }
}
