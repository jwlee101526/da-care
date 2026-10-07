package com.dacare.server.service.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dacare.server.service.diagnosis.DiagnosisMeter.Snapshot;
import com.dacare.server.service.diagnosis.DiagnosisTools.ToolProgress;
import com.openai.models.completions.CompletionUsage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * 모델 없이 회차별 응답을 흘려보내 호출 수와 토큰 합계를 검증한다.
 */
class DiagnosisMeterTests {

  private final CallAdvisorChain chain = mock(CallAdvisorChain.class);
  private final ChatClientRequest request = mock(ChatClientRequest.class);

  @Test
  void sumsEveryModelCallInsideToolLoop() {
    CompletionUsage nativeUsage = CompletionUsage.builder().promptTokens(1200)
        .completionTokens(300).totalTokens(1500)
        .completionTokensDetails(CompletionUsage.CompletionTokensDetails.builder()
            .reasoningTokens(128).build())
        .build();
    when(chain.nextCall(any()))
        .thenReturn(response("gpt-5-mini-2025-08-07",
            new DefaultUsage(1000, 50, 1050, null, null, null)))
        .thenReturn(response("gpt-5-mini-2025-08-07",
            new DefaultUsage(1200, 300, 1500, nativeUsage, 1024L, null)))
        .thenReturn(new ChatClientResponse(null, Map.of()));
    DiagnosisMeter meter = new DiagnosisMeter();

    meter.adviseCall(request, chain);
    meter.adviseCall(request, chain);
    meter.adviseCall(request, chain);
    meter.onToolProgress(new ToolProgress("searchManuals", "started"));
    meter.onToolProgress(new ToolProgress("searchManuals", "completed"));

    assertThat(meter.snapshot()).isEqualTo(
        new Snapshot("gpt-5-mini-2025-08-07", 3, 1, 2200, 1024, 350, 128));
  }

  @Test
  void runsInsideToolCallingLoop() {
    assertThat(new DiagnosisMeter().getOrder()).isGreaterThan(ToolCallingAdvisor.DEFAULT_ORDER);
  }

  private static ChatClientResponse response(String model, DefaultUsage usage) {
    ChatResponse chatResponse = ChatResponse.builder()
        .generations(List.of(new Generation(new AssistantMessage("답변"))))
        .metadata(ChatResponseMetadata.builder().model(model).usage(usage).build())
        .build();
    return new ChatClientResponse(chatResponse, Map.of());
  }
}
