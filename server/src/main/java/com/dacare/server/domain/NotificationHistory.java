package com.dacare.server.domain;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Reservation reservation;
    @Column(nullable = false) private String channel;
    @Column(nullable = false) private String status;
    @Column(nullable = false, length = 2000) private String message;
    private String failureReason;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public NotificationHistory(Reservation reservation, String channel, String status, String message, String failureReason) { this.reservation = reservation; this.channel = channel; this.status = status; this.message = message; this.failureReason = failureReason; }
}
