package com.dacare.server.api.dto.response;

import com.dacare.server.service.usage.ApiUsageService.SubjectUsage;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 요청자 기준 이번 주 AI 상담 사용량.
 */
public record SubjectUsageResponse(LocalDate periodStart, LocalDateTime resetsAt,
                                   UsageResponse diagnosis) {

  public static SubjectUsageResponse from(SubjectUsage usage) {
    return new SubjectUsageResponse(usage.periodStart(), usage.resetsAt(),
        UsageResponse.from(usage.diagnosis()));
  }
}
