package com.dacare.server.api;

import com.dacare.server.service.DiagnosisService;
import com.dacare.server.service.DiagnosisService.ConversationTurn;
import com.dacare.server.api.docs.DiagnosisApiDocs;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
  private final DiagnosisTaskRunner runner;
  private final Duration timeout;

  public DiagnosisController(DiagnosisService service, DiagnosisTaskRunner runner,
      @Value("${app.diagnosis.timeout:75s}") Duration timeout) {
    this.service = service;
    this.runner = runner;
    this.timeout = timeout;
  }

  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@Valid @RequestBody QuestionRequest request, Authentication authentication) {
    // 서버 기한 이후 오류 이벤트를 보낼 수 있도록 SSE 연결 자체는 조금 더 길게 유지한다.
    Stream stream = new Stream(new SseEmitter(timeout.plusSeconds(10).toMillis()));
    try {
      stream.start(runner.submit(() -> {
        try {
          DiagnosisService.DiagnosisResult result = service.diagnose(request.question(),
              request.history() == null ? List.of() : request.history(),
              authentication == null ? null : authentication.getName(),
              progress -> stream.progress(progress));
          stream.finish("completed", result);
        } catch (DiagnosisService.DiagnosisCancelledException exception) {
          stream.close();
        } catch (DiagnosisService.DiagnosisUnavailableException exception) {
          stream.finish("error", Map.of("message", exception.getMessage()));
        } catch (RuntimeException exception) {
          stream.finish("error", Map.of("message", "진단 요청을 처리하지 못했습니다."));
        }
      }), runner.schedule(() -> {
        stream.cancel();
        stream.finish("error", Map.of("message", "진단 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."));
      }, timeout));
    } catch (RejectedExecutionException exception) {
      stream.finish("error", Map.of("message", "상담 요청이 많아 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }
    return stream.emitter;
  }

  /**
   * 요청 하나의 SSE 응답. 작업 완료와 서버 기한 중 먼저 도착한 쪽만 종료 이벤트를 보낸다.
   */
  private static final class Stream {

    private final SseEmitter emitter;
    private final AtomicBoolean finished = new AtomicBoolean();
    private final AtomicReference<Future<?>> task = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> deadline = new AtomicReference<>();

    Stream(SseEmitter emitter) {
      this.emitter = emitter;
      // 클라이언트가 연결을 끊으면(전송 실패, 컨테이너 오류, 타임아웃) 진행 중인 모델·도구 호출을 중단한다.
      emitter.onError(ignored -> cancel());
      emitter.onTimeout(this::cancel);
    }

    void start(Future<?> task, ScheduledFuture<?> deadline) {
      this.task.set(task);
      this.deadline.set(deadline);
      if (finished.get()) {
        deadline.cancel(false);
      }
    }

    void progress(Object data) {
      if (!finished.get()) {
        send("tool", data);
      }
    }

    void finish(String name, Object data) {
      if (finished.compareAndSet(false, true)) {
        stopDeadline();
        send(name, data);
        emitter.complete();
      }
    }

    void close() {
      if (finished.compareAndSet(false, true)) {
        stopDeadline();
        emitter.complete();
      }
    }

    void cancel() {
      Future<?> running = task.get();
      if (running != null) {
        running.cancel(true);
      }
    }

    private void stopDeadline() {
      ScheduledFuture<?> scheduled = deadline.get();
      if (scheduled != null) {
        scheduled.cancel(false);
      }
    }

    private void send(String name, Object data) {
      try {
        emitter.send(SseEmitter.event().name(name).data(data));
      } catch (IOException | IllegalStateException exception) {
        // 연결이 이미 닫혔거나 완료된 응답이다.
        finished.set(true);
        stopDeadline();
        cancel();
        emitter.completeWithError(exception);
      }
    }
  }

  public record QuestionRequest(@NotBlank @Size(max = 2000) String question,
                         @Size(max = 12) List<@NotNull @Valid ConversationTurn> history) {

  }
}
