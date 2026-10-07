package com.dacare.server.service.diagnosis;

/**
 * 클라이언트 연결 종료 등으로 상담 작업이 취소됐음을 나타낸다. 도구 오류로 모델에 전달하지 않고 호출자까지 전파한다.
 */
public class DiagnosisCancelledException extends RuntimeException {

  public DiagnosisCancelledException() {
    super("진단 요청이 취소되었습니다.");
  }
}
