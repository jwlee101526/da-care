package com.dacare.server.api;

import com.dacare.server.api.docs.DiagnosisApiDocs;
import com.dacare.server.service.ApiUsageService;
import com.dacare.server.service.ApiUsageService.AcquireResult;
import com.dacare.server.service.DiagnosisExecutor;
import com.dacare.server.service.DiagnosisService;
import com.dacare.server.service.DiagnosisCancelledException;
import com.dacare.server.service.DiagnosisService.ConversationTurn;
import com.dacare.server.service.DiagnosisService.DiagnosisResult;
import com.dacare.server.service.DiagnosisService.Speaker;
import com.dacare.server.service.DiagnosisUnavailableException;
import com.dacare.server.service.UsageSubject;
import com.dacare.server.web.UsageSubjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
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
    DiagnosisStream stream = new DiagnosisStream(new SseEmitter(timeout.plusSeconds(10).toMillis()));
    UsageSubject subject = subjects.resolve(authentication, servletRequest);
    AcquireResult acquired = usage.tryAcquireDiagnosis(subject);
    if (acquired != AcquireResult.ACQUIRED) {
      stream.fail(limitCode(acquired), limitMessage(acquired, subject));
      return stream.emitter();
    }
    try {
      stream.start(executor.submit(() -> diagnose(stream, request, authentication)),
          executor.schedule(() -> {
            // 작업 중단보다 안내를 먼저 보낸다. 중단된 작업이 먼저 스트림을 닫으면 안내가 유실된다.
            stream.fail("DIAGNOSIS_TIMEOUT", "진단 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");
            stream.cancel();
          }, timeout));
    } catch (RejectedExecutionException exception) {
      usage.releaseDiagnosis(subject);
      stream.fail("DIAGNOSIS_BUSY", "상담 요청이 많아 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
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
      stream.fail("DIAGNOSIS_UNAVAILABLE", exception.getMessage());
    } catch (RuntimeException exception) {
      stream.fail("DIAGNOSIS_FAILED", "진단 요청을 처리하지 못했습니다.");
    }
  }

  private static String limitCode(AcquireResult result) {
    return result == AcquireResult.TOTAL_LIMIT_REACHED ? "SERVICE_LIMIT_EXCEEDED"
        : "WEEKLY_LIMIT_EXCEEDED";
  }

  private static String limitMessage(AcquireResult result, UsageSubject subject) {
    if (result == AcquireResult.TOTAL_LIMIT_REACHED) {
      return "이번 주 서비스 전체 AI 상담 한도가 소진되었습니다. 다음 주 월요일에 다시 이용해 주세요.";
    }
    return subject.isGuest()
        ? "이번 주 비로그인 상담 가능 횟수를 모두 사용했습니다. 로그인하면 상담을 계속 이용할 수 있습니다."
        : "이번 주 AI 상담 가능 횟수를 모두 사용했습니다. 다음 주 월요일에 다시 이용해 주세요.";
  }

  public record QuestionRequest(@NotBlank @Size(max = 2000) String question,
                         @Size(max = 12) List<@NotNull @Valid ConversationTurnRequest> history) {

    List<ConversationTurn> turns() {
      return history == null ? List.of()
          : history.stream().map(ConversationTurnRequest::toTurn).toList();
    }
  }

  public record ConversationTurnRequest(@NotNull Speaker role,
                                        @NotBlank @Size(max = 4000) String text) {

    ConversationTurn toTurn() {
      return new ConversationTurn(role, text);
    }
  }
}
