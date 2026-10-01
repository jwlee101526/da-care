package com.dacare.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import com.dacare.server.service.ApiUsageService;
import com.dacare.server.service.ApiUsageService.AcquireResult;
import com.dacare.server.service.DiagnosisExecutor;
import com.dacare.server.service.DiagnosisService;
import com.dacare.server.service.UsageSubject;
import com.dacare.server.web.UsageSubjectResolver;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.accept.ApiVersionResolver;

/**
 * 모델 호출 없이 SSE 종료 이벤트와 서버 상담 기한 처리를 검증한다.
 */
class DiagnosisControllerTests {

  private final DiagnosisService service = mock(DiagnosisService.class);
  private final DiagnosisExecutor executor = new DiagnosisExecutor(2, 2);
  private final ApiUsageService usage = mock(ApiUsageService.class);
  private final UsageSubjectResolver subjects = new UsageSubjectResolver("", "test-secret");

  @BeforeEach
  void setUp() {
    when(usage.tryAcquireDiagnosis(any(UsageSubject.class))).thenReturn(AcquireResult.ACQUIRED);
  }

  @AfterEach
  void tearDown() {
    executor.destroy();
  }

  private MockMvc mvc(Duration timeout) {
    ApiVersionResolver header = request -> request.getHeader("API-Version");
    return MockMvcBuilders.standaloneSetup(new DiagnosisController(service, executor, usage, subjects, timeout))
        .setApiVersionStrategy(new DefaultApiVersionStrategy(List.of(header),
            new SemanticApiVersionParser(), false, null, true, null, null))
        .build();
  }

  private String stream(MockMvc mvc) throws Exception {
    MvcResult result = mvc.perform(post("/api/diagnosis/chat/stream")
            .header("API-Version", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .content("{\"question\":\"노트북 전원이 켜지지 않아요\"}"))
        .andExpect(request().asyncStarted()).andReturn();
    result.getAsyncResult(5_000);
    return mvc.perform(asyncDispatch(result)).andReturn().getResponse()
        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  }

  @Test
  void completedResultIsSentAsFinalEvent() throws Exception {
    when(service.diagnose(anyString(), anyList(), isNull(), any())).thenReturn(
        new DiagnosisService.DiagnosisResult("안내된 순서대로 점검해 보세요.", List.of(), List.of()));

    assertThat(stream(mvc(Duration.ofSeconds(5))))
        .contains("event:completed").contains("안내된 순서대로 점검해 보세요.")
        .doesNotContain("event:error");
  }

  @Test
  void deadlineSendsTimeoutErrorAndInterruptsWork() throws Exception {
    CountDownLatch interrupted = new CountDownLatch(1);
    when(service.diagnose(anyString(), anyList(), isNull(), any())).thenAnswer(invocation -> {
      try {
        Thread.sleep(10_000);
      } catch (InterruptedException exception) {
        interrupted.countDown();
        throw new DiagnosisService.DiagnosisCancelledException();
      }
      return null;
    });

    assertThat(stream(mvc(Duration.ofMillis(300))))
        .contains("event:error").contains("진단 응답 시간이 초과되었습니다")
        .doesNotContain("event:completed");
    assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void dailyLimitSendsErrorWithoutCallingModel() throws Exception {
    when(usage.tryAcquireDiagnosis(any(UsageSubject.class)))
        .thenReturn(AcquireResult.SUBJECT_LIMIT_REACHED);

    assertThat(stream(mvc(Duration.ofSeconds(5))))
        .contains("event:error").contains("WEEKLY_LIMIT_EXCEEDED").contains("로그인하면")
        .doesNotContain("event:completed");
    verifyNoInteractions(service);
  }

  @Test
  void serviceLimitSendsDistinctCode() throws Exception {
    when(usage.tryAcquireDiagnosis(any(UsageSubject.class)))
        .thenReturn(AcquireResult.TOTAL_LIMIT_REACHED);

    assertThat(stream(mvc(Duration.ofSeconds(5))))
        .contains("event:error").contains("SERVICE_LIMIT_EXCEEDED")
        .doesNotContain("event:completed");
    verifyNoInteractions(service);
  }
}
