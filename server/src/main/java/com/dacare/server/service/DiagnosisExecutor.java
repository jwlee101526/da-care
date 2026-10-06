package com.dacare.server.service;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 상담 SSE 작업 전용 스레드 풀.
 * <p>
 * 모델 응답을 기다리며 오래 블로킹되는 작업이 공용 ForkJoinPool을 점유하지 않도록 분리하고, 동시 처리 수와 대기열을 제한한다. 애플리케이션 기본
 * TaskExecutor 자동 구성에 영향을 주지 않도록 Executor 빈으로 노출하지 않는다.
 */
@Component
public class DiagnosisExecutor implements DisposableBean {

  private final ThreadPoolExecutor executor;
  private final ScheduledExecutorService scheduler;

  public DiagnosisExecutor(@Value("${app.diagnosis.max-concurrency:8}") int maxConcurrency,
      @Value("${app.diagnosis.queue-capacity:16}") int queueCapacity) {
    AtomicInteger sequence = new AtomicInteger();
    this.executor = new ThreadPoolExecutor(maxConcurrency, maxConcurrency, 60, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(queueCapacity), task -> {
      Thread thread = new Thread(task, "diagnosis-" + sequence.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    });
    this.executor.allowCoreThreadTimeOut(true);
    this.scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
      Thread thread = new Thread(task, "diagnosis-deadline");
      thread.setDaemon(true);
      return thread;
    });
  }

  /**
   * @throws RejectedExecutionException 동시 처리 수와 대기열이 모두 찬 경우
   */
  public Future<?> submit(Runnable task) {
    return executor.submit(task);
  }

  public ScheduledFuture<?> schedule(Runnable task, Duration delay) {
    return scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void destroy() {
    scheduler.shutdownNow();
    executor.shutdownNow();
  }
}
