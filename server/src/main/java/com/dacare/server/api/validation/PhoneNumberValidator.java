package com.dacare.server.api.validation;

import com.dacare.server.domain.PhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

  private boolean mobile;

  @Override
  public void initialize(ValidPhoneNumber annotation) {
    mobile = annotation.mobile();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (!mobile) {
      return PhoneNumber.isValid(value);
    }
    if (PhoneNumber.isValidMobile(value)) {
      return true;
    }
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate("올바른 휴대전화 번호를 입력해 주세요.")
        .addConstraintViolation();
    return false;
  }
}
