package com.dacare.server.service;

import com.dacare.server.error.BusinessException;
import com.dacare.server.error.ErrorCode;

/**
 * 모델 설정이 없거나 모델·도구 호출에 실패해 상담을 제공할 수 없음을 나타낸다.
 */
public class DiagnosisUnavailableException extends BusinessException {

  public DiagnosisUnavailableException() {
    super(ErrorCode.DIAGNOSIS_UNAVAILABLE);
  }

  public DiagnosisUnavailableException(Throwable cause) {
    super(ErrorCode.DIAGNOSIS_UNAVAILABLE, cause);
  }
}
