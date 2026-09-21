package com.dacare.server.api.docs;

import com.dacare.server.api.ReservationController.GuestCancelRequest;
import com.dacare.server.api.ReservationController.GuestLookupRequest;
import com.dacare.server.api.ReservationController.GuestReservationRequest;
import com.dacare.server.api.ReservationController.ReservationRequest;
import com.dacare.server.api.ReservationController.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "예약", description = "회원·비회원 방문 점검 예약 관리")
public interface ReservationApiDocs {

  @Operation(summary = "회원 예약 접수", description = "로그인한 회원의 방문 점검 예약을 접수합니다.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "예약 접수 완료"),
      @ApiResponse(responseCode = "400", description = "입력값 오류")})
  ReservationResponse create(Authentication authentication,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(schema = @Schema(implementation = ReservationRequest.class), examples = @ExampleObject("""
              {"deviceType":"세탁기","symptomDescription":"탈수 시 큰 소음 발생","visitAddress":"서울특별시 강남구 테헤란로 1","preferredAt":"2026-10-01T14:00:00","contactName":"홍길동","contactPhone":"010-1234-5678"}
              """))) ReservationRequest request);

  @Operation(summary = "비회원 예약 접수", description = "연락처와 조회용 비밀번호로 비회원 예약을 접수합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "예약 접수 완료"),
      @ApiResponse(responseCode = "400", description = "입력값 오류")})
  ReservationResponse createGuest(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
      content = @Content(schema = @Schema(implementation = GuestReservationRequest.class), examples = @ExampleObject("""
          {"deviceType":"세탁기","symptomDescription":"탈수 시 큰 소음 발생","visitAddress":"서울특별시 강남구 테헤란로 1","preferredAt":"2026-10-01T14:00:00","contactName":"홍길동","contactPhone":"010-1234-5678","guestPassword":"guest1234"}
          """))) GuestReservationRequest request);

  @Operation(summary = "비회원 예약 조회", description = "예약 번호, 연락처, 조회용 비밀번호로 예약을 조회합니다.")
  ReservationResponse lookupGuest(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
      content = @Content(schema = @Schema(implementation = GuestLookupRequest.class), examples = @ExampleObject("""
          {"reservationId":1,"contactPhone":"010-1234-5678","guestPassword":"guest1234"}
          """))) GuestLookupRequest request);

  @Operation(summary = "비회원 예약 취소", description = "대기 상태의 비회원 예약을 취소합니다.")
  ReservationResponse cancelGuest(@Parameter(description = "예약 번호") Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
          content = @Content(schema = @Schema(implementation = GuestCancelRequest.class), examples = @ExampleObject("""
              {"contactPhone":"010-1234-5678","guestPassword":"guest1234"}
              """))) GuestCancelRequest request);

  @Operation(summary = "내 예약 목록 조회", description = "로그인한 회원의 예약 목록을 조회합니다.")
  @SecurityRequirement(name = "bearerAuth")
  List<ReservationResponse> mine(Authentication authentication);

  @Operation(summary = "내 예약 상세 조회", description = "로그인한 회원의 예약 한 건을 조회합니다.")
  @SecurityRequirement(name = "bearerAuth")
  ReservationResponse mineOne(Authentication authentication, @Parameter(description = "예약 번호") Long id);

  @Operation(summary = "회원 예약 취소", description = "로그인한 회원의 대기 상태 예약을 취소합니다.")
  @SecurityRequirement(name = "bearerAuth")
  ReservationResponse cancel(Authentication authentication, @Parameter(description = "예약 번호") Long id);
}
