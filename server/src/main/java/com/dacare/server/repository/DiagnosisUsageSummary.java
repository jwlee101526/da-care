package com.dacare.server.repository;

/**
 * 한 주 동안의 AI 상담 사용량 집계. 상담이 없으면 모든 값이 0이다.
 */
public record DiagnosisUsageSummary(long consultations, long completed, long failed,
                                    long timedOut, long cancelled, double averageDurationMs,
                                    double averageModelCalls, long inputTokens,
                                    long cachedInputTokens, long outputTokens,
                                    long reasoningTokens) {

}
