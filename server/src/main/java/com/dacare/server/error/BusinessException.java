package com.dacare.server.error;

/**
 * 사용자에게 그대로 안내해도 되는 업무 규칙 위반. 메시지는 응답 본문으로 나간다.
 */
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    this(errorCode, errorCode.message());
  }

  /**
   * @param message 상황에 맞게 바꾼 안내 문구. 내부 정보를 담지 않는다.
   */
  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.message(), cause);
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
