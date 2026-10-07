package com.dacare.server.api.dto.response;

import com.dacare.server.repository.DiagnosisUsageSummary;
import com.dacare.server.service.usage.ApiUsageService.TotalUsage;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 서비스 전체 이번 주 유료 API 사용량.
 */
public record TotalUsageResponse(LocalDate periodStart, LocalDateTime resetsAt,
                                 UsageResponse diagnosis, UsageResponse sms,
                                 DiagnosisMetricsResponse diagnosisMetrics) {

  public static TotalUsageResponse from(TotalUsage usage, DiagnosisUsageSummary metrics) {
    return new TotalUsageResponse(usage.periodStart(), usage.resetsAt(),
        UsageResponse.from(usage.diagnosis()), UsageResponse.from(usage.sms()),
        DiagnosisMetricsResponse.from(metrics));
  }
}
