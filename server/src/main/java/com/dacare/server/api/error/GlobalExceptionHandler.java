package com.dacare.server.api.error;

import com.dacare.server.error.BusinessException;
import com.dacare.server.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ErrorResponse> business(BusinessException e) {
    ErrorCode errorCode = e.errorCode();
    return ResponseEntity.status(errorCode.status())
        .body(ErrorResponse.of(errorCode, e.getMessage()));
  }

  /**
   * 값 객체(휴대전화 번호 등)의 형식 오류. 업무 규칙 위반은 {@link BusinessException}으로 표현한다.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ErrorResponse> illegal(IllegalArgumentException e) {
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
        .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, e.getMessage()));
  }

  /**
   * 본문이 JSON이 아니거나, 정해진 값만 받는 항목(기기 분류 등)에 알 수 없는 값이 온 경우.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
        .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult().getFieldErrors()
        .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status())
        .body(ErrorResponse.withFields(ErrorCode.VALIDATION_ERROR, fields));
  }
}
