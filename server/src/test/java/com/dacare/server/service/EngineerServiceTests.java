package com.dacare.server.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.error.BusinessException;
import com.dacare.server.error.ErrorCode;
import com.dacare.server.repository.EngineerRepository;
import com.dacare.server.repository.ReservationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EngineerServiceTests {

  private final EngineerRepository engineers = mock(EngineerRepository.class);
  private final ReservationRepository reservations = mock(ReservationRepository.class);
  private final EngineerService service = new EngineerService(engineers, reservations);
  private final Engineer engineer = new Engineer("김기사", PhoneNumber.ofMobile("010-1234-5678"),
      "가전", "서울");

  @Test
  void assignedEngineerCannotBeDeleted() {
    when(engineers.findById(1L)).thenReturn(Optional.of(engineer));
    when(reservations.existsByEngineer(engineer)).thenReturn(true);

    assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.ENGINEER_IN_USE);
    verify(engineers, never()).delete(any());
  }

  @Test
  void unassignedEngineerIsDeleted() {
    when(engineers.findById(1L)).thenReturn(Optional.of(engineer));
    when(reservations.existsByEngineer(engineer)).thenReturn(false);

    service.delete(1L);

    verify(engineers).delete(engineer);
  }

  @Test
  void missingEngineerIsNotFound() {
    when(engineers.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ErrorCode.ENGINEER_NOT_FOUND);
  }
}
