package com.dacare.server.service.diagnosis;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 실행 중인 상담 작업 하나와 그 서버 기한. 작업이 끝나면 기한을 해제하고, 호출자가 연결을 끊으면 작업을 중단한다.
 */
public final class DiagnosisSession {

  private final AtomicBoolean finished = new AtomicBoolean();
  private final AtomicReference<Future<?>> task = new AtomicReference<>();
  private final AtomicReference<ScheduledFuture<?>> deadline = new AtomicReference<>();

  static DiagnosisSession finishedSession() {
    DiagnosisSession session = new DiagnosisSession();
    session.finished.set(true);
    return session;
  }

  void start(Future<?> task, ScheduledFuture<?> deadline) {
    this.task.set(task);
    this.deadline.set(deadline);
    if (finished.get()) {
      deadline.cancel(false);
    }
  }

  void finish() {
    finished.set(true);
    ScheduledFuture<?> scheduled = deadline.get();
    if (scheduled != null) {
      scheduled.cancel(false);
    }
  }

  public void cancel() {
    Future<?> running = task.get();
    if (running != null) {
      running.cancel(true);
    }
  }
}
