package com.dacare.server.repository;

import com.dacare.server.domain.ApiUsage;
import com.dacare.server.domain.PaidApi;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ApiUsageRepository extends JpaRepository<ApiUsage, Long> {

  Optional<ApiUsage> findByApiAndUsageDate(PaidApi api, LocalDate usageDate);

  /**
   * 한도 미만일 때만 1 증가시킨다. 증가하면 1, 한도에 도달했거나 해당 일자 행이 없으면 0을 반환한다.
   */
  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("update ApiUsage u set u.used = u.used + 1 "
      + "where u.api = :api and u.usageDate = :date and u.used < :limit")
  int increment(@Param("api") PaidApi api, @Param("date") LocalDate date,
      @Param("limit") int limit);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("update ApiUsage u set u.used = u.used - 1 "
      + "where u.api = :api and u.usageDate = :date and u.used > 0")
  int decrement(@Param("api") PaidApi api, @Param("date") LocalDate date);
}
