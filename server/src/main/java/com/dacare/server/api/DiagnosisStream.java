package com.dacare.server.api;

import com.dacare.server.api.error.ErrorResponse;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 요청 하나의 SSE 응답. 작업 완료와 서버 기한 중 먼저 도착한 쪽만 종료 이벤트를 보낸다.
 */
final class DiagnosisStream {

  private final SseEmitter emitter;
  private final AtomicBoolean finished = new AtomicBoolean();
  private final AtomicReference<Future<?>> task = new AtomicReference<>();
  private final AtomicReference<ScheduledFuture<?>> deadline = new AtomicReference<>();

  DiagnosisStream(SseEmitter emitter) {
    this.emitter = emitter;
    // 클라이언트가 연결을 끊으면(전송 실패, 컨테이너 오류, 타임아웃) 진행 중인 모델·도구 호출을 중단한다.
    emitter.onError(ignored -> cancel());
    emitter.onTimeout(this::cancel);
  }

  SseEmitter emitter() {
    return emitter;
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

  void fail(String code, String message) {
    finish("error", ErrorResponse.of(code, message));
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
