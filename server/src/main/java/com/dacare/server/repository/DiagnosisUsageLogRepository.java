package com.dacare.server.repository;

import com.dacare.server.domain.DiagnosisUsageLog;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiagnosisUsageLogRepository extends JpaRepository<DiagnosisUsageLog, Long> {

  @Query("select new com.dacare.server.repository.DiagnosisUsageSummary(count(l), "
      + "coalesce(sum(case when l.outcome = com.dacare.server.domain.DiagnosisOutcome.COMPLETED then 1L else 0L end), 0L), "
      + "coalesce(sum(case when l.outcome = com.dacare.server.domain.DiagnosisOutcome.FAILED then 1L else 0L end), 0L), "
      + "coalesce(sum(case when l.outcome = com.dacare.server.domain.DiagnosisOutcome.TIMEOUT then 1L else 0L end), 0L), "
      + "coalesce(sum(case when l.outcome = com.dacare.server.domain.DiagnosisOutcome.CANCELLED then 1L else 0L end), 0L), "
      + "coalesce(avg(l.durationMs), 0.0), coalesce(avg(l.modelCalls), 0.0), "
      + "coalesce(sum(l.inputTokens), 0L), coalesce(sum(l.cachedInputTokens), 0L), "
      + "coalesce(sum(l.outputTokens), 0L), coalesce(sum(l.reasoningTokens), 0L)) "
      + "from DiagnosisUsageLog l where l.periodStart = :period")
  DiagnosisUsageSummary summarize(@Param("period") LocalDate period);
}
