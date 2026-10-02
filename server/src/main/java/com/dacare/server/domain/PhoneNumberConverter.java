package com.dacare.server.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link PhoneNumber}를 정규화한 문자열 컬럼으로 저장한다.
 */
@Converter(autoApply = true)
public class PhoneNumberConverter implements AttributeConverter<PhoneNumber, String> {

  @Override
  public String convertToDatabaseColumn(PhoneNumber phone) {
    return phone == null ? null : phone.value();
  }

  @Override
  public PhoneNumber convertToEntityAttribute(String stored) {
    return stored == null ? null : PhoneNumber.restore(stored);
  }
}
