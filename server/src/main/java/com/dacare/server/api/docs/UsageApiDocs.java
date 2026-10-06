package com.dacare.server.api.docs;

import com.dacare.server.api.dto.response.SubjectUsageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

@Tag(name = "Usage", description = "유료 API 주간 사용량 API")
public interface UsageApiDocs {

  @Operation(summary = "내 이번 주 AI 상담 사용량 조회",
      description = "요청자 기준(로그인 사용자는 계정, 비로그인 사용자는 IP) 이번 주 AI 상담 사용 횟수와 한도를 반환합니다. "
          + "남은 횟수는 서비스 전체 잔여량을 넘지 않습니다. 매주 월요일 0시(한국 시간)에 초기화됩니다.")
  @ApiResponse(responseCode = "200", description = "조회 완료")
  SubjectUsageResponse mine(@Parameter(hidden = true) Authentication authentication,
      @Parameter(hidden = true) HttpServletRequest request);
}
