package com.dacare.server.repository;

import com.dacare.server.domain.Customer;
import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.Reservation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  List<Reservation> findAllByCustomerOrderByCreatedAtDesc(Customer customer);

  List<Reservation> findAllByOrderByCreatedAtDesc();

  Optional<Reservation> findByCode(String code);

  boolean existsByCode(String code);

  boolean existsByEngineer(Engineer engineer);
}
