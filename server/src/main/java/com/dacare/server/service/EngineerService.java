package com.dacare.server.service;

import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.error.BusinessException;
import com.dacare.server.error.ErrorCode;
import com.dacare.server.repository.EngineerRepository;
import com.dacare.server.repository.ReservationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EngineerService {

  private final EngineerRepository engineers;
  private final ReservationRepository reservations;

  public EngineerService(EngineerRepository engineers, ReservationRepository reservations) {
    this.engineers = engineers;
    this.reservations = reservations;
  }

  public List<Engineer> all() {
    return engineers.findAll();
  }

  @Transactional
  public Engineer create(String name, PhoneNumber phone, String specialty, String region) {
    return engineers.save(new Engineer(name, phone, specialty, region));
  }

  @Transactional
  public Engineer update(Long id, String name, PhoneNumber phone, String specialty, String region) {
    Engineer engineer = getEngineer(id);
    engineer.update(name, phone, specialty, region);
    return engineer;
  }

  /**
   * 예약 이력이 기사를 참조하므로, 배정된 예약이 한 건이라도 있으면 삭제하지 않는다.
   */
  @Transactional
  public void delete(Long id) {
    Engineer engineer = getEngineer(id);
    if (reservations.existsByEngineer(engineer)) {
      throw new BusinessException(ErrorCode.ENGINEER_IN_USE);
    }
    engineers.delete(engineer);
  }

  private Engineer getEngineer(Long id) {
    return engineers.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.ENGINEER_NOT_FOUND));
  }
}
