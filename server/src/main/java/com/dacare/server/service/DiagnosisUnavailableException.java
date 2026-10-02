package com.dacare.server.service;

/**
 * 모델 설정이 없거나 모델·도구 호출에 실패해 상담을 제공할 수 없음을 나타낸다.
 */
public class DiagnosisUnavailableException extends RuntimeException {

  private static final String MESSAGE = "진단 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해 주세요.";

  public DiagnosisUnavailableException() {
    super(MESSAGE);
  }

  public DiagnosisUnavailableException(Throwable cause) {
    super(MESSAGE, cause);
  }
}
