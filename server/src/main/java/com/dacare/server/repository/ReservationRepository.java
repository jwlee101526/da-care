package com.dacare.server.repository;
import com.dacare.server.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReservationRepository extends JpaRepository<Reservation, Long> { List<Reservation> findAllByCustomerOrderByCreatedAtDesc(Customer customer); }
