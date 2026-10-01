package com.dacare.server.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * API 오류 응답 본문. REST 오류와 SSE error 이벤트가 같은 형태를 사용한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, String> fields) {

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(code, message, null);
  }
}
