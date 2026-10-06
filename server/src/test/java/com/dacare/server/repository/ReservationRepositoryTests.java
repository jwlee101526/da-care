package com.dacare.server.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dacare.server.domain.AppUser;
import com.dacare.server.domain.Customer;
import com.dacare.server.domain.DeviceType;
import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.domain.Reservation;
import com.dacare.server.domain.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * 예약 목록 응답이 고객·기사 정보를 함께 쓰므로, 목록 조회가 예약 수만큼 추가 쿼리를 내지 않는지 확인한다.
 */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:reservation-repository;MODE=PostgreSQL",
    "spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ReservationRepositoryTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);

  @Autowired
  private ReservationRepository reservations;
  @Autowired
  private EntityManager entityManager;
  @Autowired
  private EntityManagerFactory entityManagerFactory;

  private Customer customer;

  @BeforeEach
  void setUp() {
    AppUser user = new AppUser("customer@test.local", "hash", Role.CUSTOMER);
    entityManager.persist(user);
    customer = new Customer(user, "홍길동", PhoneNumber.ofMobile("010-1111-2222"), "서울");
    entityManager.persist(customer);
    for (int i = 0; i < 3; i++) {
      Engineer engineer = new Engineer("기사" + i, PhoneNumber.ofMobile("010-3333-000" + i), "가전",
          "서울");
      entityManager.persist(engineer);
      Reservation reservation = Reservation.forCustomer("1000000" + i, customer, DeviceType.tv, "화면이 꺼짐",
          "서울", NOW.plusDays(1), "홍길동", customer.getPhone(), NOW.plusMinutes(i));
      reservation.confirm(engineer, NOW.plusDays(2));
      entityManager.persist(reservation);
    }
    entityManager.flush();
    entityManager.clear();
    statistics().clear();
  }

  @Test
  void allReservationsAreLoadedInOneQuery() {
    List<Reservation> found = reservations.findAllByOrderByCreatedAtDesc();

    assertThat(found).hasSize(3).allSatisfy(reservation -> {
      assertThat(reservation.getEngineer().getName()).startsWith("기사");
      assertThat(reservation.getCustomer().getUser().getEmail()).isEqualTo("customer@test.local");
    });
    assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
  }

  @Test
  void customerReservationsAreLoadedInOneQuery() {
    List<Reservation> found = reservations.findAllByCustomerOrderByCreatedAtDesc(customer);

    assertThat(found).hasSize(3)
        .allSatisfy(reservation -> assertThat(reservation.getEngineer()).isNotNull());
    assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
  }

  private Statistics statistics() {
    return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
  }
}
