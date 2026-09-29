package com.dacare.server.api;

import com.dacare.server.service.DiagnosisService;
import com.dacare.server.service.DiagnosisService.ConversationTurn;
import com.dacare.server.api.docs.DiagnosisApiDocs;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
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
  private final DiagnosisTaskRunner runner;

  public DiagnosisController(DiagnosisService service, DiagnosisTaskRunner runner) {
    this.service = service;
    this.runner = runner;
  }

  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@Valid @RequestBody QuestionRequest request, Authentication authentication) {
    SseEmitter emitter = new SseEmitter(100_000L);
    AtomicReference<Future<?>> task = new AtomicReference<>();
    // 클라이언트가 연결을 끊으면(전송 실패, 컨테이너 오류, 타임아웃) 진행 중인 모델·도구 호출을 중단한다.
    Runnable cancel = () -> {
      Future<?> running = task.get();
      if (running != null) {
        running.cancel(true);
      }
    };
    emitter.onError(ignored -> cancel.run());
    emitter.onTimeout(cancel);
    try {
      task.set(runner.submit(() -> {
        try {
          DiagnosisService.DiagnosisResult result = service.diagnose(request.question(),
              request.history() == null ? List.of() : request.history(),
              authentication == null ? null : authentication.getName(),
              progress -> send(emitter, "tool", progress, cancel));
          send(emitter, "completed", result, cancel);
          emitter.complete();
        } catch (DiagnosisService.DiagnosisCancelledException exception) {
          emitter.complete();
        } catch (DiagnosisService.DiagnosisUnavailableException exception) {
          send(emitter, "error", Map.of("message", exception.getMessage()), cancel);
          emitter.complete();
        } catch (RuntimeException exception) {
          send(emitter, "error", Map.of("message", "진단 요청을 처리하지 못했습니다."), cancel);
          emitter.complete();
        }
      }));
    } catch (RejectedExecutionException exception) {
      send(emitter, "error", Map.of("message", "상담 요청이 많아 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."),
          cancel);
      emitter.complete();
    }
    return emitter;
  }

  private static void send(SseEmitter emitter, String name, Object data, Runnable cancel) {
    try {
      emitter.send(SseEmitter.event().name(name).data(data));
    } catch (IOException | IllegalStateException exception) {
      // 연결이 이미 닫혔거나 완료된 응답이다.
      cancel.run();
      emitter.completeWithError(exception);
    }
  }

  public record QuestionRequest(@NotBlank @Size(max = 2000) String question,
                         @Size(max = 12) List<@NotNull @Valid ConversationTurn> history) {

  }
}
