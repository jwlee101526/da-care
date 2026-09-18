package com.dacare.server.domain;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = true) @JoinColumn(name = "customer_id", nullable = true) private Customer customer;
    @ManyToOne private Engineer engineer;
    @Column(nullable = false) private String deviceType;
    @Column(nullable = false, length = 2000) private String symptomDescription;
    @Column(nullable = false) private String visitAddress;
    @Column(nullable = false) private LocalDateTime preferredAt;
    private LocalDateTime confirmedAt;
    private String contactName;
    private String contactPhone;
    @Column(length = 100) private String guestPasswordHash;
    public void setContact(String name, String phone) { this.contactName = name; this.contactPhone = phone; }
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Reservation(Customer customer, String deviceType, String symptomDescription, String visitAddress, LocalDateTime preferredAt) { this.customer = customer; this.deviceType = deviceType; this.symptomDescription = symptomDescription; this.visitAddress = visitAddress; this.preferredAt = preferredAt; this.status = ReservationStatus.PENDING; }
    public Reservation(String deviceType, String symptomDescription, String visitAddress, LocalDateTime preferredAt, String contactName, String contactPhone, String guestPasswordHash) { this.customer = null; this.deviceType = deviceType; this.symptomDescription = symptomDescription; this.visitAddress = visitAddress; this.preferredAt = preferredAt; this.contactName = contactName; this.contactPhone = contactPhone; this.guestPasswordHash = guestPasswordHash; this.status = ReservationStatus.PENDING; }
    public boolean isGuest() { return this.customer == null; }
    public void confirm(Engineer engineer, LocalDateTime confirmedAt) { if (status != ReservationStatus.PENDING) throw new IllegalStateException("대기 중인 예약만 확정할 수 있습니다."); this.engineer = engineer; this.confirmedAt = confirmedAt; this.status = ReservationStatus.CONFIRMED; }
    public void cancel() { if (status != ReservationStatus.PENDING) throw new IllegalStateException("대기 중인 예약만 취소할 수 있습니다."); this.status = ReservationStatus.CANCELLED; }
    public void complete() { if (status != ReservationStatus.CONFIRMED) throw new IllegalStateException("확정된 예약만 완료 처리할 수 있습니다."); this.status = ReservationStatus.COMPLETED; }
    public void cancelByAdmin() { if (status == ReservationStatus.COMPLETED || status == ReservationStatus.CANCELLED) throw new IllegalStateException("완료 또는 취소된 예약은 취소할 수 없습니다."); this.status = ReservationStatus.CANCELLED; }
}
