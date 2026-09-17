package com.dacare.server.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "user_id", unique = true) private AppUser user;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String phone;
    @Column(nullable = false) private String address;
    public Customer(AppUser user, String name, String phone, String address) { this.user = user; this.name = name; this.phone = phone; this.address = address; }
}
