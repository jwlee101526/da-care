package com.dacare.server.api.error;

import com.dacare.server.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * API 오류 응답 본문. REST 오류와 SSE error 이벤트가 같은 형태를 사용한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, String> fields) {

  public static ErrorResponse of(ErrorCode errorCode) {
    return of(errorCode, errorCode.message());
  }

  public static ErrorResponse of(ErrorCode errorCode, String message) {
    return new ErrorResponse(errorCode.name(), message, null);
  }

  public static ErrorResponse withFields(ErrorCode errorCode, Map<String, String> fields) {
    return new ErrorResponse(errorCode.name(), errorCode.message(), fields);
  }
}
