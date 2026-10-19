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

  static final int MESSAGE_MAX_LENGTH = 2000;
  static final int FAILURE_REASON_MAX_LENGTH = 255;

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
  @Column(nullable = false, length = MESSAGE_MAX_LENGTH)
  private String message;
  @Column(length = FAILURE_REASON_MAX_LENGTH)
  private String failureReason;
  @Column(nullable = false)
  private LocalDateTime createdAt;

  /**
   * 알림 문구에는 사용자가 입력한 증상·주소가, 실패 사유에는 외부 API의 오류 메시지가 그대로 들어가 컬럼 길이를 넘을 수 있다. 이력 저장이 실패하지 않도록
   * 컬럼 길이에 맞춰 자른다.
   */
  public NotificationHistory(Reservation reservation, NotificationChannel channel,
      NotificationStatus status, String message, String failureReason, LocalDateTime createdAt) {
    this.reservation = reservation;
    this.channel = channel;
    this.status = status;
    this.message = truncate(message, MESSAGE_MAX_LENGTH);
    this.failureReason = truncate(failureReason, FAILURE_REASON_MAX_LENGTH);
    this.createdAt = createdAt;
  }

  /**
   * 이모지 같은 서로게이트 쌍이 반으로 잘리지 않게 자른다.
   */
  private static String truncate(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    int end = Character.isHighSurrogate(text.charAt(maxLength - 1)) ? maxLength - 1 : maxLength;
    return text.substring(0, end);
  }
}
