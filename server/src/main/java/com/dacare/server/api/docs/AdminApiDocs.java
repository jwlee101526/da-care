package com.dacare.server.api.docs;

import com.dacare.server.api.dto.request.ConfirmationRequest;
import com.dacare.server.api.dto.request.EngineerRequest;
import com.dacare.server.api.dto.response.EngineerResponse;
import com.dacare.server.api.dto.response.ReservationResponse;
import com.dacare.server.service.usage.ApiUsageService.TotalUsage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Admin", description = "기사 및 예약 운영 API")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "로그인 필요")
@ApiResponse(responseCode = "403", description = "관리자 권한 없음")
public interface AdminApiDocs {

  @Operation(summary = "기사 목록 조회")
  @ApiResponse(responseCode = "200", description = "조회 완료")
  List<EngineerResponse> engineers();

  @Operation(summary = "기사 등록")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "기사 등록 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류")
      }
  )
  EngineerResponse create(
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.ENGINEER)))
      EngineerRequest request);

  @Operation(summary = "기사 수정")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "기사 수정 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "404", description = "기사 없음")
      }
  )
  EngineerResponse update(@Parameter(description = "기사 번호") Long id, EngineerRequest request);

  @Operation(summary = "기사 삭제")
  @ApiResponse(responseCode = "200", description = "기사 삭제 완료")
  void delete(@Parameter(description = "기사 번호") Long id);

  @Operation(summary = "서비스 전체 이번 주 사용량 조회",
      description = "AI 상담과 SMS의 이번 주 서비스 전체 사용 횟수와 주간 한도를 반환합니다.")
  @ApiResponse(responseCode = "200", description = "조회 완료")
  TotalUsage usage();

  @Operation(summary = "전체 예약 조회")
  @ApiResponse(responseCode = "200", description = "조회 완료")
  List<ReservationResponse> reservations();

  @Operation(summary = "예약 확정 및 기사 배정")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "예약 확정 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류 또는 지난 방문 일시"),
          @ApiResponse(responseCode = "404", description = "예약 또는 기사 없음"),
          @ApiResponse(responseCode = "409", description = "확정할 수 없는 예약 상태")
      }
  )
  ReservationResponse confirm(@Parameter(description = "예약 번호") Long id,
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.CONFIRMATION)))
      ConfirmationRequest request);

  @Operation(summary = "점검 완료 처리")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "점검 완료 처리 완료"),
          @ApiResponse(responseCode = "404", description = "예약 없음"),
          @ApiResponse(responseCode = "409", description = "완료할 수 없는 예약 상태")
      }
  )
  ReservationResponse complete(@Parameter(description = "예약 번호") Long id);

  @Operation(summary = "예약 취소")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "예약 취소 완료"),
          @ApiResponse(responseCode = "404", description = "예약 없음"),
          @ApiResponse(responseCode = "409", description = "취소할 수 없는 예약 상태")
      }
  )
  ReservationResponse cancel(@Parameter(description = "예약 번호") Long id);
}
