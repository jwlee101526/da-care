package com.dacare.server.service.diagnosis;

import com.dacare.server.service.diagnosis.DiagnosisTools.ToolProgress;
import com.openai.models.completions.CompletionUsage;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 상담 한 건의 모델 호출 수와 토큰 사용량을 센다. 상담마다 새로 만든다.
 * <p>
 * 도구 호출 루프보다 안쪽에 두어 실제 모델 호출을 회차마다 하나씩 본다. 최종 응답에 의존하지 않으므로 실패·시간 초과로 끝난 상담도 그때까지
 * 사용한 토큰이 남는다. 시간 초과 스레드가 읽을 수 있어 접근을 동기화한다.
 */
public class DiagnosisMeter implements CallAdvisor {

  private int modelCalls;
  private int toolCalls;
  private long inputTokens;
  private long cachedInputTokens;
  private long outputTokens;
  private long reasoningTokens;
  private String model;

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    ChatClientResponse response = chain.nextCall(request);
    record(response.chatResponse());
    return response;
  }

  @Override
  public String getName() {
    return "DiagnosisMeter";
  }

  @Override
  public int getOrder() {
    return ToolCallingAdvisor.DEFAULT_ORDER + 1;
  }

  public synchronized void onToolProgress(ToolProgress progress) {
    if ("started".equals(progress.status())) {
      toolCalls++;
    }
  }

  public synchronized Snapshot snapshot() {
    return new Snapshot(model, modelCalls, toolCalls, inputTokens, cachedInputTokens,
        outputTokens, reasoningTokens);
  }

  private synchronized void record(ChatResponse response) {
    modelCalls++;
    if (response == null) {
      return;
    }
    if (response.getMetadata().getModel() != null && !response.getMetadata().getModel().isBlank()) {
      model = response.getMetadata().getModel();
    }
    Usage usage = response.getMetadata().getUsage();
    if (usage == null) {
      return;
    }
    inputTokens += value(usage.getPromptTokens());
    outputTokens += value(usage.getCompletionTokens());
    cachedInputTokens += value(usage.getCacheReadInputTokens());
    if (usage.getNativeUsage() instanceof CompletionUsage completion) {
      reasoningTokens += completion.completionTokensDetails()
          .flatMap(CompletionUsage.CompletionTokensDetails::reasoningTokens).orElse(0L);
    }
  }

  private static long value(Number number) {
    return number == null ? 0 : number.longValue();
  }

  public record Snapshot(String model, int modelCalls, int toolCalls, long inputTokens,
                         long cachedInputTokens, long outputTokens, long reasoningTokens) {

  }
}
