package com.dacare.server.api.docs;

import com.dacare.server.api.AdminController.ConfirmationRequest;
import com.dacare.server.api.AdminController.EngineerRequest;
import com.dacare.server.api.AdminController.EngineerResponse;
import com.dacare.server.api.ReservationController.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "관리자", description = "기사 및 예약 운영")
@SecurityRequirement(name = "bearerAuth")
public interface AdminApiDocs {

  @Operation(summary = "기사 목록 조회")
  List<EngineerResponse> engineers();

  @Operation(summary = "기사 등록")
  @ApiResponse(responseCode = "200", description = "기사 등록 완료")
  EngineerResponse create(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
      content = @Content(schema = @Schema(implementation = EngineerRequest.class), examples = @ExampleObject("""
          {"name":"김기사","phone":"010-9876-5432","specialty":"생활가전","region":"서울"}
          """))) EngineerRequest request);

  @Operation(summary = "기사 수정")
  EngineerResponse update(@Parameter(description = "기사 번호") Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(schema = @Schema(implementation = EngineerRequest.class))) EngineerRequest request);

  @Operation(summary = "기사 삭제")
  @ApiResponse(responseCode = "200", description = "기사 삭제 완료")
  void delete(@Parameter(description = "기사 번호") Long id);

  @Operation(summary = "전체 예약 조회")
  List<ReservationResponse> reservations();

  @Operation(summary = "예약 확정 및 기사 배정")
  ReservationResponse confirm(@Parameter(description = "예약 번호") Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(schema = @Schema(implementation = ConfirmationRequest.class), examples = @ExampleObject("""
              {"engineerId":1,"confirmedAt":"2026-10-01T14:00:00"}
              """))) ConfirmationRequest request);

  @Operation(summary = "점검 완료 처리")
  ReservationResponse complete(@Parameter(description = "예약 번호") Long id);

  @Operation(summary = "예약 취소")
  ReservationResponse cancel(@Parameter(description = "예약 번호") Long id);
}
