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
 * 유료 API의 일자별 서비스 전체 호출 횟수.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_api_usage_api_date",
    columnNames = {"api", "usage_date"}))
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
  @Column(nullable = false)
  private LocalDate usageDate;
  @Column(nullable = false)
  private int used;

  public ApiUsage(PaidApi api, LocalDate usageDate, int used) {
    this.api = api;
    this.usageDate = usageDate;
    this.used = used;
  }
}
