package com.dacare.server.service;

import com.dacare.server.domain.AppUser;
import com.dacare.server.domain.Customer;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.domain.Role;
import com.dacare.server.repository.AppUserRepository;
import com.dacare.server.repository.CustomerRepository;
import com.dacare.server.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final String INVALID_CREDENTIALS = "이메일 또는 비밀번호가 올바르지 않습니다.";

  private final AppUserRepository users;
  private final CustomerRepository customers;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthService(AppUserRepository users, CustomerRepository customers, PasswordEncoder encoder,
      JwtService jwt) {
    this.users = users;
    this.customers = customers;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  @Transactional
  public String signup(String email, String password, String name, PhoneNumber phone, String address) {
    if (users.existsByEmail(email)) {
      throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
    }
    AppUser user = users.save(new AppUser(email, encoder.encode(password), Role.CUSTOMER));
    customers.save(new Customer(user, name, phone, address));
    return jwt.createToken(email, Role.CUSTOMER.name());
  }

  @Transactional(readOnly = true)
  public String login(String email, String password) {
    AppUser user = users.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException(INVALID_CREDENTIALS));
    if (!encoder.matches(password, user.getPasswordHash())) {
      throw new IllegalArgumentException(INVALID_CREDENTIALS);
    }
    return jwt.createToken(user.getEmail(), user.getRole().name());
  }
}
