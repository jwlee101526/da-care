package com.dacare.server.error;

import org.springframework.http.HttpStatus;

/**
 * 업무 규칙 위반의 종류. 응답 상태와 오류 코드, 기본 안내 문구를 함께 정한다.
 */
public enum ErrorCode {
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
  EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
  CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "고객 정보를 찾을 수 없습니다."),
  ENGINEER_NOT_FOUND(HttpStatus.NOT_FOUND, "기사를 찾을 수 없습니다."),
  RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
  GUEST_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND,
      "예약 정보를 찾을 수 없습니다. 예약 번호와 휴대전화 번호를 확인해 주세요."),
  INVALID_SCHEDULE(HttpStatus.BAD_REQUEST, "방문 일시는 미래여야 합니다."),
  INVALID_RESERVATION_STATE(HttpStatus.CONFLICT, "현재 예약 상태에서는 처리할 수 없습니다."),
  TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "조회 시도가 너무 많습니다. 15분 후 다시 시도해 주세요."),
  DIAGNOSIS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "진단 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해 주세요.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  public HttpStatus status() {
    return status;
  }

  public String message() {
    return message;
  }
}
