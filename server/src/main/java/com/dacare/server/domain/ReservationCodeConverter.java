package com.dacare.server.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link ReservationCode}를 숫자 8자리 문자열 컬럼으로 저장한다.
 */
@Converter(autoApply = true)
public class ReservationCodeConverter implements AttributeConverter<ReservationCode, String> {

  @Override
  public String convertToDatabaseColumn(ReservationCode code) {
    return code == null ? null : code.value();
  }

  @Override
  public ReservationCode convertToEntityAttribute(String stored) {
    return stored == null ? null : ReservationCode.restore(stored);
  }
}
