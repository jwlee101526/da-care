package com.dacare.server.domain;

import com.dacare.server.error.BusinessException;
import com.dacare.server.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, unique = true, updatable = false, length = ReservationCode.LENGTH)
  private String code;
  @ManyToOne(optional = true)
  @JoinColumn(name = "customer_id", nullable = true)
  private Customer customer;
  @ManyToOne
  private Engineer engineer;
  @Column(nullable = false)
  private String deviceType;
  @Column(nullable = false, length = 2000)
  private String symptomDescription;
  @Column(nullable = false)
  private String visitAddress;
  @Column(nullable = false)
  private LocalDateTime preferredAt;
  private LocalDateTime confirmedAt;
  private String contactName;
  private PhoneNumber contactPhone;
  /** 완료 또는 취소된 시각. 비회원 조회 가능 기간을 이 시각부터 센다. */
  private LocalDateTime closedAt;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(Types.VARCHAR)
  @Column(nullable = false, length = 20)
  private ReservationStatus status;
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  private Reservation(String code, Customer customer, String deviceType,
      String symptomDescription, String visitAddress, LocalDateTime preferredAt,
      String contactName, PhoneNumber contactPhone) {
    this.code = code;
    this.customer = customer;
    this.deviceType = deviceType;
    this.symptomDescription = symptomDescription;
    this.visitAddress = visitAddress;
    this.preferredAt = preferredAt;
    this.contactName = contactName;
    this.contactPhone = contactPhone;
    this.status = ReservationStatus.PENDING;
  }

  /** 회원 예약. 고객 계정과 연결되며 '내 예약'에서 조회한다. */
  public static Reservation forCustomer(String code, Customer customer, String deviceType,
      String symptomDescription, String visitAddress, LocalDateTime preferredAt,
      String contactName, PhoneNumber contactPhone) {
    return new Reservation(code, customer, deviceType, symptomDescription, visitAddress,
        preferredAt, contactName, contactPhone);
  }

  /** 비회원 예약. 예약 번호와 휴대전화 번호로만 조회할 수 있다. */
  public static Reservation forGuest(String code, String deviceType, String symptomDescription,
      String visitAddress, LocalDateTime preferredAt, String contactName,
      PhoneNumber contactPhone) {
    return new Reservation(code, null, deviceType, symptomDescription, visitAddress,
        preferredAt, contactName, contactPhone);
  }

  public boolean isGuest() {
    return this.customer == null;
  }

  public void confirm(Engineer engineer, LocalDateTime confirmedAt) {
    if (status != ReservationStatus.PENDING) {
      throw new BusinessException(ErrorCode.INVALID_RESERVATION_STATE,
          "대기 중인 예약만 확정할 수 있습니다.");
    }
    this.engineer = engineer;
    this.confirmedAt = confirmedAt;
    this.status = ReservationStatus.CONFIRMED;
  }

  public void cancel(LocalDateTime now) {
    if (status != ReservationStatus.PENDING) {
      throw new BusinessException(ErrorCode.INVALID_RESERVATION_STATE,
          "대기 중인 예약만 취소할 수 있습니다.");
    }
    close(ReservationStatus.CANCELLED, now);
  }

  public void complete(LocalDateTime now) {
    if (status != ReservationStatus.CONFIRMED) {
      throw new BusinessException(ErrorCode.INVALID_RESERVATION_STATE,
          "확정된 예약만 완료 처리할 수 있습니다.");
    }
    close(ReservationStatus.COMPLETED, now);
  }

  public void cancelByAdmin(LocalDateTime now) {
    if (status == ReservationStatus.COMPLETED || status == ReservationStatus.CANCELLED) {
      throw new BusinessException(ErrorCode.INVALID_RESERVATION_STATE,
          "완료 또는 취소된 예약은 취소할 수 없습니다.");
    }
    close(ReservationStatus.CANCELLED, now);
  }

  private void close(ReservationStatus status, LocalDateTime now) {
    this.status = status;
    this.closedAt = now;
  }
}
