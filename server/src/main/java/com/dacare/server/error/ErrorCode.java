package com.dacare.server.error;

import org.springframework.http.HttpStatus;

/**
 * API 오류의 종류. 응답 상태와 오류 코드, 기본 안내 문구를 함께 정한다.
 */
public enum ErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인하세요."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
  EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
  CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "고객 정보를 찾을 수 없습니다."),
  ENGINEER_NOT_FOUND(HttpStatus.NOT_FOUND, "기사를 찾을 수 없습니다."),
  ENGINEER_IN_USE(HttpStatus.CONFLICT, "배정된 예약이 있는 기사는 삭제할 수 없습니다."),
  RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
  GUEST_RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND,
      "예약 정보를 찾을 수 없습니다. 예약 번호와 휴대전화 번호를 확인해 주세요."),
  INVALID_SCHEDULE(HttpStatus.BAD_REQUEST, "방문 일시는 미래여야 합니다."),
  INVALID_RESERVATION_STATE(HttpStatus.CONFLICT, "현재 예약 상태에서는 처리할 수 없습니다."),
  TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "조회 시도가 너무 많습니다. 15분 후 다시 시도해 주세요."),
  DIAGNOSIS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "진단 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해 주세요."),
  DIAGNOSIS_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "상담 요청이 많아 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."),
  DIAGNOSIS_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "진단 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."),
  DIAGNOSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "진단 요청을 처리하지 못했습니다."),
  SERVICE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
      "이번 주 서비스 전체 AI 상담 한도가 소진되었습니다. 다음 주 월요일에 다시 이용해 주세요."),
  WEEKLY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
      "이번 주 AI 상담 가능 횟수를 모두 사용했습니다. 다음 주 월요일에 다시 이용해 주세요.");

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
