package com.dacare.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.sql.Types;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(optional = false)
  private Reservation reservation;
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(Types.VARCHAR)
  @Column(nullable = false)
  private NotificationChannel channel;
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(Types.VARCHAR)
  @Column(nullable = false)
  private NotificationStatus status;
  @Column(nullable = false, length = 2000)
  private String message;
  private String failureReason;
  @Column(nullable = false)
  private LocalDateTime createdAt;

  public NotificationHistory(Reservation reservation, NotificationChannel channel,
      NotificationStatus status, String message, String failureReason, LocalDateTime createdAt) {
    this.reservation = reservation;
    this.channel = channel;
    this.status = status;
    this.message = message;
    this.failureReason = failureReason;
    this.createdAt = createdAt;
  }
}
