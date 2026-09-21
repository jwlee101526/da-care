package com.dacare.server.repository;

import com.dacare.server.domain.Customer;
import com.dacare.server.domain.Reservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  List<Reservation> findAllByCustomerOrderByCreatedAtDesc(Customer customer);
}
