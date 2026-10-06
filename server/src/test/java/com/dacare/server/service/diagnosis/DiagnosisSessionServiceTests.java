package com.dacare.server.service.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dacare.server.error.ErrorCode;
import com.dacare.server.service.diagnosis.DiagnosisService.DiagnosisResult;
import com.dacare.server.service.diagnosis.DiagnosisTools.ToolProgress;
import com.dacare.server.service.usage.ApiUsageService.AcquireResult;
import com.dacare.server.service.usage.ApiUsageService;
import com.dacare.server.service.usage.UsageSubject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 모델 호출 없이 상담 한도 차감·되돌리기와 종료 통지를 검증한다.
 */
class DiagnosisSessionServiceTests {

  private final DiagnosisService service = mock(DiagnosisService.class);
  private final ApiUsageService usage = mock(ApiUsageService.class);
  private final UsageSubject subject = UsageSubject.guest("ip-hash");
  private final DiagnosisExecutor executor = new DiagnosisExecutor(2, 2);

  @BeforeEach
  void setUp() {
    when(service.isAvailable()).thenReturn(true);
    when(usage.tryAcquireDiagnosis(subject)).thenReturn(AcquireResult.ACQUIRED);
  }

  @Test
  void unavailableModelIsRejectedWithoutChargingUsage() {
    when(service.isAvailable()).thenReturn(false);
    RecordingListener listener = new RecordingListener();

    new DiagnosisSessionService(service, executor, usage, Duration.ofSeconds(5))
        .start("질문", List.of(), subject, null, listener);

    assertThat(listener.result.join()).isEqualTo(ErrorCode.DIAGNOSIS_UNAVAILABLE.name());
    verify(usage, never()).tryAcquireDiagnosis(subject);
  }

  @AfterEach
  void tearDown() {
    executor.destroy();
  }

  @Test
  void rejectedTaskReleasesUsage() {
    DiagnosisExecutor busy = mock(DiagnosisExecutor.class);
    when(busy.submit(any())).thenThrow(new RejectedExecutionException());
    RecordingListener listener = new RecordingListener();

    new DiagnosisSessionService(service, busy, usage, Duration.ofSeconds(5))
        .start("질문", List.of(), subject, null, listener);

    assertThat(listener.result.join()).isEqualTo(ErrorCode.DIAGNOSIS_BUSY.name());
    verify(usage).releaseDiagnosis(subject);
  }

  @Test
  void unexpectedFailureIsReportedAsDiagnosisFailed() throws Exception {
    when(service.diagnose(anyString(), anyList(), isNull(), any()))
        .thenThrow(new IllegalStateException("boom"));
    RecordingListener listener = new RecordingListener();

    new DiagnosisSessionService(service, executor, usage, Duration.ofSeconds(5))
        .start("질문", List.of(), subject, null, listener);

    assertThat(listener.result.get(2, TimeUnit.SECONDS))
        .isEqualTo(ErrorCode.DIAGNOSIS_FAILED.name());
    verify(usage, never()).releaseDiagnosis(subject);
  }

  @Test
  void completedResultIsReported() throws Exception {
    when(service.diagnose(anyString(), anyList(), isNull(), any()))
        .thenReturn(new DiagnosisResult("점검해 보세요.", List.of(), List.of()));
    RecordingListener listener = new RecordingListener();

    new DiagnosisSessionService(service, executor, usage, Duration.ofSeconds(5))
        .start("질문", List.of(), subject, null, listener);

    assertThat(listener.result.get(2, TimeUnit.SECONDS)).isEqualTo("completed");
  }

  /**
   * 처음 받은 종료 통지를 기록한다.
   */
  private static final class RecordingListener implements DiagnosisListener {

    private final CompletableFuture<String> result = new CompletableFuture<>();

    @Override
    public void progress(ToolProgress progress) {
    }

    @Override
    public void completed(DiagnosisResult diagnosisResult) {
      result.complete("completed");
    }

    @Override
    public void failed(ErrorCode errorCode, String message) {
      result.complete(errorCode.name());
    }

    @Override
    public void cancelled() {
      result.complete("cancelled");
    }
  }
}
