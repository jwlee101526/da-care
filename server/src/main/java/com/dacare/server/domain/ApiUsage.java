package com.dacare.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.sql.Types;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * 유료 API의 주간 호출 횟수. 대상(subject)은 서비스 전체, 계정, 비로그인 IP 중 하나다.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_api_usage_api_subject_period",
    columnNames = {"api", "subject", "period_start"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiUsage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(Types.VARCHAR)
  @Column(nullable = false, length = 30)
  private PaidApi api;
  @Column(nullable = false, length = 320)
  private String subject;
  /**
   * 집계 주의 시작일(월요일, 한국 시간).
   */
  @Column(nullable = false)
  private LocalDate periodStart;
  @Column(nullable = false)
  private int used;

  public ApiUsage(PaidApi api, String subject, LocalDate periodStart, int used) {
    this.api = api;
    this.subject = subject;
    this.periodStart = periodStart;
    this.used = used;
  }
}
