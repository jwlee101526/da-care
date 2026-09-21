package com.dacare.server.api;

import com.dacare.server.service.DiagnosisService;
import com.dacare.server.service.DiagnosisService.ConversationTurn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(path = "/api/diagnosis", version = "1")
public class DiagnosisController {

  private final DiagnosisService service;

  public DiagnosisController(DiagnosisService service) {
    this.service = service;
  }

  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter stream(@Valid @RequestBody QuestionRequest request, Authentication authentication) {
    SseEmitter emitter = new SseEmitter(100_000L);
    CompletableFuture.runAsync(() -> {
      try {
        DiagnosisService.DiagnosisResult result = service.diagnose(request.question(),
            request.history() == null ? List.of() : request.history(),
            authentication == null ? null : authentication.getName(),
            progress -> send(emitter, "tool", progress));
        send(emitter, "completed", result);
        emitter.complete();
      } catch (DiagnosisService.DiagnosisUnavailableException exception) {
        send(emitter, "error", Map.of("message", exception.getMessage()));
        emitter.complete();
      } catch (RuntimeException exception) {
        send(emitter, "error", Map.of("message", "진단 요청을 처리하지 못했습니다."));
        emitter.complete();
      }
    });
    return emitter;
  }

  private static void send(SseEmitter emitter, String name, Object data) {
    try {
      emitter.send(SseEmitter.event().name(name).data(data));
    } catch (IOException exception) {
      emitter.completeWithError(exception);
    }
  }

  record QuestionRequest(@NotBlank @Size(max = 2000) String question,
                         @Size(max = 12) List<@NotNull @Valid ConversationTurn> history) {

  }
}
