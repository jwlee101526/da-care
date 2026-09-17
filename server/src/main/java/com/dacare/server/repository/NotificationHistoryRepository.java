package com.dacare.server.repository;
import com.dacare.server.domain.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {}
