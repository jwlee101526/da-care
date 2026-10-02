package com.dacare.server.api.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 전화번호 입력값 검증. 규칙은 {@link com.dacare.server.domain.PhoneNumber}와 같다. null은 통과하므로 필수 입력에는
 * {@code @NotBlank}를 함께 붙인다. {@link #mobile()}이 true면 휴대전화 번호만 허용한다.
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
public @interface ValidPhoneNumber {

  String message() default "올바른 전화번호를 입력해 주세요.";

  boolean mobile() default false;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
