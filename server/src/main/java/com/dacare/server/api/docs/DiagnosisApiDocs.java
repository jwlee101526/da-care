package com.dacare.server.api.docs;

import com.dacare.server.api.DiagnosisController.QuestionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI Diagnosis", description = "SSE 기반 기기 증상 상담 API")
public interface DiagnosisApiDocs {

  @Operation(summary = "AI 진단 스트림 시작",
      description = "SSE로 진행 상태(`tool`), 진단 완료(`completed`), 오류(`error`) 이벤트를 반환합니다. "
          + "주간 한도를 넘으면 모델을 호출하지 않고 `error` 이벤트로 `WEEKLY_LIMIT_EXCEEDED`(사용자 한도) 또는 "
          + "`SERVICE_LIMIT_EXCEEDED`(서비스 전체 한도) 코드를 보냅니다.")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 완료",
              content = @Content(mediaType = "text/event-stream",
                  examples = @ExampleObject(ApiExamples.DIAGNOSIS_STREAM))),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "503", description = "진단 서비스 이용 불가")
      }
  )
  SseEmitter stream(
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.QUESTION)))
      QuestionRequest request,
      @Parameter(hidden = true) Authentication authentication,
      @Parameter(hidden = true) HttpServletRequest servletRequest);
}
