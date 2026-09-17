package com.dacare.server.domain;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Customer customer;
    @ManyToOne private Engineer engineer;
    @Column(nullable = false) private String deviceType;
    @Column(nullable = false, length = 2000) private String symptomDescription;
    @Column(nullable = false) private String visitAddress;
    @Column(nullable = false) private LocalDateTime preferredAt;
    private LocalDateTime confirmedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Reservation(Customer customer, String deviceType, String symptomDescription, String visitAddress, LocalDateTime preferredAt) { this.customer = customer; this.deviceType = deviceType; this.symptomDescription = symptomDescription; this.visitAddress = visitAddress; this.preferredAt = preferredAt; this.status = ReservationStatus.PENDING; }
    public void confirm(Engineer engineer, LocalDateTime confirmedAt) { this.engineer = engineer; this.confirmedAt = confirmedAt; this.status = ReservationStatus.CONFIRMED; }
}
