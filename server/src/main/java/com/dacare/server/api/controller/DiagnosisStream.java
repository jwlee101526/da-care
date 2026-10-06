package com.dacare.server.api.controller;

import com.dacare.server.api.error.ErrorResponse;
import com.dacare.server.error.ErrorCode;
import com.dacare.server.service.diagnosis.DiagnosisListener;
import com.dacare.server.service.diagnosis.DiagnosisService.DiagnosisResult;
import com.dacare.server.service.diagnosis.DiagnosisSession;
import com.dacare.server.service.diagnosis.DiagnosisTools.ToolProgress;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 요청 하나의 SSE 응답. 먼저 도착한 종료 통지 하나만 종료 이벤트로 보낸다.
 */
final class DiagnosisStream implements DiagnosisListener {

  private final SseEmitter emitter;
  private final AtomicBoolean finished = new AtomicBoolean();
  private final AtomicReference<DiagnosisSession> session = new AtomicReference<>();
  private final AtomicBoolean cancelRequested = new AtomicBoolean();

  DiagnosisStream(SseEmitter emitter) {
    this.emitter = emitter;
    // 클라이언트가 연결을 끊으면(전송 실패, 컨테이너 오류, 타임아웃) 진행 중인 모델·도구 호출을 중단한다.
    emitter.onError(ignored -> cancel());
    emitter.onTimeout(this::cancel);
  }

  SseEmitter emitter() {
    return emitter;
  }

  void attach(DiagnosisSession session) {
    this.session.set(session);
    // 세션을 받기 전에 연결이 끊겼다면 바로 중단한다.
    if (cancelRequested.get()) {
      session.cancel();
    }
  }

  @Override
  public void progress(ToolProgress progress) {
    if (!finished.get()) {
      send("tool", progress);
    }
  }

  @Override
  public void completed(DiagnosisResult result) {
    finish("completed", result);
  }

  @Override
  public void failed(ErrorCode errorCode, String message) {
    finish("error", ErrorResponse.of(errorCode, message));
  }

  @Override
  public void cancelled() {
    if (finished.compareAndSet(false, true)) {
      emitter.complete();
    }
  }

  private void finish(String name, Object data) {
    if (finished.compareAndSet(false, true)) {
      send(name, data);
      emitter.complete();
    }
  }

  private void cancel() {
    cancelRequested.set(true);
    DiagnosisSession running = session.get();
    if (running != null) {
      running.cancel();
    }
  }

  private void send(String name, Object data) {
    try {
      emitter.send(SseEmitter.event().name(name).data(data));
    } catch (IOException | IllegalStateException exception) {
      // 연결이 이미 닫혔거나 완료된 응답이다.
      finished.set(true);
      cancel();
      emitter.completeWithError(exception);
    }
  }
}
