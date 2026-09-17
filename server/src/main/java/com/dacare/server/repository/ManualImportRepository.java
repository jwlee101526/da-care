package com.dacare.server.repository;
import com.dacare.server.domain.ManualImport;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ManualImportRepository extends JpaRepository<ManualImport, Long> { boolean existsBySource(String source); }
