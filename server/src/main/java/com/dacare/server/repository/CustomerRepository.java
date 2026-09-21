package com.dacare.server.repository;

import com.dacare.server.domain.AppUser;
import com.dacare.server.domain.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  Optional<Customer> findByUser(AppUser user);
}
