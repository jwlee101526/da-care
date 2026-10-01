package com.dacare.server.api.docs;

import com.dacare.server.service.ApiUsageService.DailyUsage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Usage", description = "유료 API 일일 사용량 API")
public interface UsageApiDocs {

  @Operation(summary = "오늘 사용량 조회",
      description = "AI 상담과 SMS의 오늘(한국 시간 기준) 서비스 전체 사용 횟수와 하루 한도를 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 완료")
  })
  DailyUsage today();
}
