package com.dacare.server.api.dto.response;

import com.dacare.server.repository.DiagnosisUsageSummary;

/**
 * 이번 주 AI 상담의 모델 사용량 집계. 실패·시간 초과·취소된 상담의 토큰도 포함한다.
 */
public record DiagnosisMetricsResponse(long consultations, long completed, long failed,
                                       long timedOut, long cancelled, long averageDurationMs,
                                       double averageModelCalls, long inputTokens,
                                       long cachedInputTokens, long outputTokens,
                                       long reasoningTokens) {

  static DiagnosisMetricsResponse from(DiagnosisUsageSummary summary) {
    return new DiagnosisMetricsResponse(summary.consultations(), summary.completed(),
        summary.failed(), summary.timedOut(), summary.cancelled(),
        Math.round(summary.averageDurationMs()),
        Math.round(summary.averageModelCalls() * 10) / 10.0, summary.inputTokens(),
        summary.cachedInputTokens(), summary.outputTokens(), summary.reasoningTokens());
  }
}
