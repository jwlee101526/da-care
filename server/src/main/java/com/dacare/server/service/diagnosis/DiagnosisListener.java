package com.dacare.server.service.diagnosis;

import com.dacare.server.error.ErrorCode;
import com.dacare.server.service.diagnosis.DiagnosisService.DiagnosisResult;
import com.dacare.server.service.diagnosis.DiagnosisTools.ToolProgress;

/**
 * 상담 진행 상황을 받는 쪽. 종료 통지(completed·failed·cancelled)는 여러 번 올 수 있으므로 먼저 받은 하나만 반영해야 한다.
 */
public interface DiagnosisListener {

  void progress(ToolProgress progress);

  void completed(DiagnosisResult result);

  void failed(ErrorCode errorCode, String message);

  default void failed(ErrorCode errorCode) {
    failed(errorCode, errorCode.message());
  }

  void cancelled();
}
