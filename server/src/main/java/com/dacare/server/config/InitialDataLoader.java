package com.dacare.server.config;

import com.dacare.server.domain.AppUser;
import com.dacare.server.domain.Customer;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.domain.Role;
import com.dacare.server.repository.AppUserRepository;
import com.dacare.server.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기동 시 운영 관리자 계정을 보장하고, 데모 모드에서는 체험용 계정을 만든다.
 * <p>
 * {@code @PostConstruct}에서는 트랜잭션 프록시가 적용되지 않으므로 컨텍스트 준비 후 실행되는 러너로 둔다.
 */
@Component
class InitialDataLoader implements ApplicationRunner {

  private static final String DEMO_ADMIN_EMAIL = "admin@dacare.com";
  private static final String DEMO_ADMIN_PASSWORD = "admin1234";
  private static final String DEMO_CUSTOMER_EMAIL = "demo@dacare.com";
  private static final String DEMO_CUSTOMER_PASSWORD = "password1234";

  private final AppUserRepository users;
  private final CustomerRepository customers;
  private final PasswordEncoder encoder;
  private final String adminEmail;
  private final String adminPassword;
  private final boolean demoDataEnabled;

  InitialDataLoader(AppUserRepository users, CustomerRepository customers, PasswordEncoder encoder,
      @Value("${app.admin.email}") String adminEmail,
      @Value("${app.admin.password}") String adminPassword,
      @Value("${app.demo-data.enabled:false}") boolean demoDataEnabled) {
    this.users = users;
    this.customers = customers;
    this.encoder = encoder;
    this.adminEmail = adminEmail;
    this.adminPassword = adminPassword;
    this.demoDataEnabled = demoDataEnabled;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    users.findByEmail(adminEmail).ifPresentOrElse(
        admin -> admin.updateRole(Role.ADMIN),
        () -> users.save(new AppUser(adminEmail, encoder.encode(adminPassword), Role.ADMIN)));
    if (demoDataEnabled) {
      seedDemoAccounts();
    }
  }

  private void seedDemoAccounts() {
    users.findByEmail(DEMO_ADMIN_EMAIL).ifPresentOrElse(admin -> {
      admin.updatePassword(encoder.encode(DEMO_ADMIN_PASSWORD));
      admin.updateRole(Role.ADMIN);
    }, () -> users.save(
        new AppUser(DEMO_ADMIN_EMAIL, encoder.encode(DEMO_ADMIN_PASSWORD), Role.ADMIN)));
    if (users.findByEmail(DEMO_CUSTOMER_EMAIL).isEmpty()) {
      AppUser demo = users.save(
          new AppUser(DEMO_CUSTOMER_EMAIL, encoder.encode(DEMO_CUSTOMER_PASSWORD), Role.CUSTOMER));
      customers.save(
          new Customer(demo, "홍길동", PhoneNumber.ofMobile("010-1234-5678"), "서울특별시 강남구 테헤란로 123"));
    }
  }
}
