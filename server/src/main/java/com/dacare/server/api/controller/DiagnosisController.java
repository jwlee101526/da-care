package com.dacare.server.api.controller;

import com.dacare.server.api.docs.DiagnosisApiDocs;
import com.dacare.server.api.dto.request.QuestionRequest;
import com.dacare.server.service.diagnosis.DiagnosisSessionService;
import com.dacare.server.web.UsageSubjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

  private final DiagnosisSessionService sessions;
  private final UsageSubjectResolver subjects;

  public DiagnosisController(DiagnosisSessionService sessions, UsageSubjectResolver subjects) {
    this.sessions = sessions;
    this.subjects = subjects;
  }

  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@Valid @RequestBody QuestionRequest request,
      Authentication authentication, HttpServletRequest servletRequest) {
    // 서버 기한 이후 오류 이벤트를 보낼 수 있도록 SSE 연결 자체는 조금 더 길게 유지한다.
    DiagnosisStream stream = new DiagnosisStream(
        new SseEmitter(sessions.timeout().plusSeconds(10).toMillis()));
    stream.attach(sessions.start(request.question(), request.turns(),
        subjects.resolve(authentication, servletRequest),
        authentication == null ? null : authentication.getName(), stream));
    return stream.emitter();
  }
}
