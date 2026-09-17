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
    public AuthService(AppUserRepository users, CustomerRepository customers, PasswordEncoder encoder, JwtService jwt) { this.users = users; this.customers = customers; this.encoder = encoder; this.jwt = jwt; }
    @PostConstruct @Transactional void seedAdmin() { if (!users.existsByEmail(adminEmail)) users.save(new AppUser(adminEmail, encoder.encode(adminPassword), Role.ADMIN)); }
    @Transactional public String signup(String email, String password, String name, String phone, String address) { if (users.existsByEmail(email)) throw new IllegalArgumentException("이미 사용 중인 이메일입니다."); AppUser user = users.save(new AppUser(email, encoder.encode(password), Role.CUSTOMER)); customers.save(new Customer(user, name, phone, address)); return jwt.createToken(email, Role.CUSTOMER.name()); }
    public String login(String email, String password) { AppUser user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.")); if (!encoder.matches(password, user.getPasswordHash())) throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."); return jwt.createToken(user.getEmail(), user.getRole().name()); }
}
