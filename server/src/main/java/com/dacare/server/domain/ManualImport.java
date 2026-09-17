package com.dacare.server.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManualImport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String source;
    @Column(nullable = false) private LocalDateTime importedAt = LocalDateTime.now();
    public ManualImport(String source) { this.source = source; }
}
