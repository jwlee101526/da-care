package com.dacare.server.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Engineer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String phone;
    @Column(nullable = false) private String specialty;
    @Column(nullable = false) private String region;
    public Engineer(String name, String phone, String specialty, String region) { this.name = name; this.phone = phone; this.specialty = specialty; this.region = region; }
    public void update(String name, String phone, String specialty, String region) { this.name = name; this.phone = phone; this.specialty = specialty; this.region = region; }
}
