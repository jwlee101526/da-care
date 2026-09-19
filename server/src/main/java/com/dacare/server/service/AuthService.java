package com.dacare.server.service;
import com.dacare.server.config.JwtService;
import com.dacare.server.domain.*;
import com.dacare.server.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository users; private final CustomerRepository customers; private final PasswordEncoder encoder; private final JwtService jwt;
    @Value("${app.admin.email}") private String adminEmail; @Value("${app.admin.password}") private String adminPassword;
    @Value("${app.demo-data.enabled:false}") private boolean demoDataEnabled;
    public AuthService(AppUserRepository users, CustomerRepository customers, PasswordEncoder encoder, JwtService jwt) { this.users = users; this.customers = customers; this.encoder = encoder; this.jwt = jwt; }
    @PostConstruct @Transactional void seedAdmin() {
        AppUser adminUser = users.findByEmail(adminEmail).orElse(null);
        if (adminUser == null) {
            users.save(new AppUser(adminEmail, encoder.encode(adminPassword), Role.ADMIN));
        } else {
            adminUser.updateRole(Role.ADMIN);
            users.save(adminUser);
        }
        if (!demoDataEnabled) return;
        String demoCustomerEmail = "demo@dacare.com";
        String demoAdminEmail = "admin@dacare.com";
        AppUser demoAdmin = users.findByEmail(demoAdminEmail).orElse(null);
        if (demoAdmin == null) {
            users.save(new AppUser(demoAdminEmail, encoder.encode("admin1234"), Role.ADMIN));
        } else {
            demoAdmin.updatePassword(encoder.encode("admin1234"));
            demoAdmin.updateRole(Role.ADMIN);
            users.save(demoAdmin);
        }
        AppUser demoUser = users.findByEmail(demoCustomerEmail).orElse(null);
        if (demoUser == null) {
            AppUser saved = users.save(new AppUser(demoCustomerEmail, encoder.encode("password1234"), Role.CUSTOMER));
            customers.save(new Customer(saved, "홍길동", "010-1234-5678", "서울특별시 강남구 테헤란로 123"));
        }
    }
    @Transactional public String signup(String email, String password, String name, String phone, String address) { if (users.existsByEmail(email)) throw new IllegalArgumentException("이미 사용 중인 이메일입니다."); AppUser user = users.save(new AppUser(email, encoder.encode(password), Role.CUSTOMER)); customers.save(new Customer(user, name, phone, address)); return jwt.createToken(email, Role.CUSTOMER.name()); }
    public String login(String email, String password) { AppUser user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.")); if (!encoder.matches(password, user.getPasswordHash())) throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."); return jwt.createToken(user.getEmail(), user.getRole().name()); }
}
