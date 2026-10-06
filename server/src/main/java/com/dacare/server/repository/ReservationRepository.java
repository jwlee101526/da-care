package com.dacare.server.repository;

import com.dacare.server.domain.Customer;
import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.Reservation;
import com.dacare.server.domain.ReservationCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  /**
   * 목록 응답이 고객과 기사 정보를 함께 쓰므로 한 번에 읽어 예약 수만큼 추가 조회가 나가지 않게 한다.
   */
  @EntityGraph(attributePaths = {"customer.user", "engineer"})
  List<Reservation> findAllByCustomerOrderByCreatedAtDesc(Customer customer);

  @EntityGraph(attributePaths = {"customer.user", "engineer"})
  List<Reservation> findAllByOrderByCreatedAtDesc();

  Optional<Reservation> findByCode(ReservationCode code);

  boolean existsByCode(ReservationCode code);

  boolean existsByEngineer(Engineer engineer);
}
