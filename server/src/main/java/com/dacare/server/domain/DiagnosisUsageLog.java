package com.dacare.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * AI 상담 한 건의 모델 사용량. 개선 효과를 측정하려고 남기며, 사용자를 식별하는 값은 저장하지 않는다.
 */
@Entity
@Table(indexes = @Index(name = "idx_diagnosis_usage_log_period", columnList = "period_start"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisUsageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  /**
   * 요청자 구분(CUSTOMER, ADMIN, GUEST).
   */
  @Column(nullable = false, length = 10)
  private String subjectType;
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(Types.VARCHAR)
  @Column(nullable = false, length = 10)
  private DiagnosisOutcome outcome;
  @Column(length = 100)
  private String model;
  @Column(nullable = false)
  private int modelCalls;
  @Column(nullable = false)
  private int toolCalls;
  @Column(nullable = false)
  private long durationMs;
  @Column(nullable = false)
  private long inputTokens;
  @Column(nullable = false)
  private long cachedInputTokens;
  @Column(nullable = false)
  private long outputTokens;
  @Column(nullable = false)
  private long reasoningTokens;
  /**
   * 집계 주의 시작일(월요일, 한국 시간). 주간 한도와 같은 기준이다.
   */
  @Column(nullable = false)
  private LocalDate periodStart;
  @Column(nullable = false)
  private LocalDateTime createdAt;

  public DiagnosisUsageLog(String subjectType, DiagnosisOutcome outcome, String model,
      int modelCalls, int toolCalls, long durationMs, long inputTokens, long cachedInputTokens,
      long outputTokens, long reasoningTokens, LocalDate periodStart, LocalDateTime createdAt) {
    this.subjectType = subjectType;
    this.outcome = outcome;
    this.model = model;
    this.modelCalls = modelCalls;
    this.toolCalls = toolCalls;
    this.durationMs = durationMs;
    this.inputTokens = inputTokens;
    this.cachedInputTokens = cachedInputTokens;
    this.outputTokens = outputTokens;
    this.reasoningTokens = reasoningTokens;
    this.periodStart = periodStart;
    this.createdAt = createdAt;
  }
}
