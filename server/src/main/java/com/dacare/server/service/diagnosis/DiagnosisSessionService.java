package com.dacare.server.service.diagnosis;

import com.dacare.server.error.ErrorCode;
import com.dacare.server.service.diagnosis.DiagnosisService.ConversationTurn;
import com.dacare.server.service.usage.ApiUsageService.AcquireResult;
import com.dacare.server.service.usage.ApiUsageService;
import com.dacare.server.service.usage.UsageSubject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
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
  private final Duration timeout;

  public DiagnosisSessionService(DiagnosisService service, DiagnosisExecutor executor,
      ApiUsageService usage, @Value("${app.diagnosis.timeout}") Duration timeout) {
    this.service = service;
    this.executor = executor;
    this.usage = usage;
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
    try {
      session.start(executor.submit(() -> run(session, question, history, email, listener)),
          executor.schedule(() -> {
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
      String email, DiagnosisListener listener) {
    try {
      listener.completed(service.diagnose(question, history, email, listener::progress));
    } catch (DiagnosisCancelledException exception) {
      listener.cancelled();
    } catch (DiagnosisUnavailableException exception) {
      listener.failed(exception.errorCode(), exception.getMessage());
    } catch (RuntimeException exception) {
      listener.failed(ErrorCode.DIAGNOSIS_FAILED);
    } finally {
      session.finish();
    }
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
