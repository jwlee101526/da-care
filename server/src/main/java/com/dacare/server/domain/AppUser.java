package com.dacare.server.domain;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Types;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "app_users")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(Types.VARCHAR) @Column(nullable = false, length = 20) private Role role;
    public AppUser(String email, String passwordHash, Role role) { this.email = email; this.passwordHash = passwordHash; this.role = role; }
    public void updatePassword(String passwordHash) { this.passwordHash = passwordHash; }
    public void updateRole(Role role) { this.role = role; }
}
