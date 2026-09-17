package com.dacare.server.repository;
import com.dacare.server.domain.Engineer;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EngineerRepository extends JpaRepository<Engineer, Long> {}
