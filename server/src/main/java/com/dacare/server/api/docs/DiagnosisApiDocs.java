package com.dacare.server.api.docs;

import com.dacare.server.api.DiagnosisController.QuestionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI Diagnosis", description = "SSE 기반 기기 증상 상담 API")
public interface DiagnosisApiDocs {

  @Operation(summary = "AI 진단 스트림 시작", description = "SSE로 진행 상태(`tool`), 진단 완료(`completed`), 오류(`error`) 이벤트를 반환합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "SSE 스트림 연결 완료",
          content = @Content(
          mediaType = "text/event-stream", examples = @ExampleObject("""
          event: tool
          data: {"message":"매뉴얼을 검색하고 있습니다."}
          
          event: completed
          data: {"answer":"..."}
          """))),
      @ApiResponse(responseCode = "400", description = "입력값 오류"),
      @ApiResponse(responseCode = "503", description = "진단 서비스 이용 불가")
  })
  SseEmitter stream(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          content = @Content(
              schema = @Schema(implementation = QuestionRequest.class),
              examples = @ExampleObject(
                  """
                      {"question":"세탁기에서 탈수할 때 큰 소리가 납니다.","history":[]}
                      """)
          )
      ) QuestionRequest request, Authentication authentication
  );
}
