package com.dacare.server.api.controller;

import com.dacare.server.api.docs.DiagnosisApiDocs;
import com.dacare.server.error.ErrorCode;
import com.dacare.server.api.dto.request.QuestionRequest;
import com.dacare.server.service.ApiUsageService.AcquireResult;
import com.dacare.server.service.ApiUsageService;
import com.dacare.server.service.diagnosis.DiagnosisCancelledException;
import com.dacare.server.service.diagnosis.DiagnosisExecutor;
import com.dacare.server.service.diagnosis.DiagnosisService.DiagnosisResult;
import com.dacare.server.service.diagnosis.DiagnosisService;
import com.dacare.server.service.diagnosis.DiagnosisUnavailableException;
import com.dacare.server.service.UsageSubject;
import com.dacare.server.web.UsageSubjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(path = "/api/diagnosis", version = "1")
public class DiagnosisController implements DiagnosisApiDocs {

  private final DiagnosisService service;
  private final DiagnosisExecutor executor;
  private final ApiUsageService usage;
  private final UsageSubjectResolver subjects;
  private final Duration timeout;

  public DiagnosisController(DiagnosisService service, DiagnosisExecutor executor,
      ApiUsageService usage, UsageSubjectResolver subjects,
      @Value("${app.diagnosis.timeout}") Duration timeout) {
    this.service = service;
    this.executor = executor;
    this.usage = usage;
    this.subjects = subjects;
    this.timeout = timeout;
  }

  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@Valid @RequestBody QuestionRequest request,
      Authentication authentication, HttpServletRequest servletRequest) {
    // 서버 기한 이후 오류 이벤트를 보낼 수 있도록 SSE 연결 자체는 조금 더 길게 유지한다.
    DiagnosisStream stream = new DiagnosisStream(
        new SseEmitter(timeout.plusSeconds(10).toMillis()));
    UsageSubject subject = subjects.resolve(authentication, servletRequest);
    AcquireResult acquired = usage.tryAcquireDiagnosis(subject);
    if (acquired != AcquireResult.ACQUIRED) {
      stream.fail(limitError(acquired), limitMessage(acquired, subject));
      return stream.emitter();
    }
    try {
      stream.start(executor.submit(() -> diagnose(stream, request, authentication)),
          executor.schedule(() -> {
            // 작업 중단보다 안내를 먼저 보낸다. 중단된 작업이 먼저 스트림을 닫으면 안내가 유실된다.
            stream.fail(ErrorCode.DIAGNOSIS_TIMEOUT);
            stream.cancel();
          }, timeout));
    } catch (RejectedExecutionException exception) {
      usage.releaseDiagnosis(subject);
      stream.fail(ErrorCode.DIAGNOSIS_BUSY);
    }
    return stream.emitter();
  }

  private void diagnose(DiagnosisStream stream, QuestionRequest request,
      Authentication authentication) {
    try {
      DiagnosisResult result = service.diagnose(request.question(), request.turns(),
          authentication == null ? null : authentication.getName(), stream::progress);
      stream.finish("completed", result);
    } catch (DiagnosisCancelledException exception) {
      stream.close();
    } catch (DiagnosisUnavailableException exception) {
      stream.fail(exception.errorCode(), exception.getMessage());
    } catch (RuntimeException exception) {
      stream.fail(ErrorCode.DIAGNOSIS_FAILED);
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
