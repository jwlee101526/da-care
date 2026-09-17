package com.dacare.server.repository;
import com.dacare.server.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerRepository extends JpaRepository<Customer, Long> { Optional<Customer> findByUser(AppUser user); }
