package com.dacare.server.api.docs;

import com.dacare.server.api.dto.request.GuestCancelRequest;
import com.dacare.server.api.dto.request.GuestLookupRequest;
import com.dacare.server.api.dto.request.GuestReservationRequest;
import com.dacare.server.api.dto.request.ReservationRequest;
import com.dacare.server.api.dto.response.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.Authentication;

@Tag(name = "Reservation", description = "회원/비회원 방문 점검 예약 관리 API")
public interface ReservationApiDocs {

  @Operation(summary = "회원 예약 접수", description = "로그인한 회원의 방문 점검 예약을 접수합니다.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "예약 접수 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류 또는 지난 방문 일시"),
          @ApiResponse(responseCode = "401", description = "로그인 필요")
      }
  )
  ReservationResponse create(Authentication authentication,
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.RESERVATION)))
      ReservationRequest request);

  @Operation(summary = "비회원 예약 접수", description = "연락처로 비회원 예약을 접수합니다. 응답의 code(숫자 8자리)가 고객에게 안내할 예약 번호입니다.")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "예약 접수 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류 또는 지난 방문 일시")
      }
  )
  ReservationResponse createGuest(
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.RESERVATION)))
      GuestReservationRequest request);

  @Operation(summary = "비회원 예약 조회",
      description = "예약 번호와 연락처로 비회원 예약 한 건을 조회합니다. 진행 중이거나 완료·취소 후 90일 이내인 예약만 "
          + "조회되며, 이름·연락처·상세 주소는 일부 가려서 응답합니다. 어느 값이 틀렸는지는 구분하지 않고 404로 응답하며, "
          + "실패가 반복되면 15분간 429로 거부합니다.")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "조회 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "404", description = "일치하는 비회원 예약 없음"),
          @ApiResponse(responseCode = "429", description = "조회 실패 횟수 초과")
      }
  )
  ReservationResponse lookupGuest(
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.GUEST_LOOKUP)))
      GuestLookupRequest request,
      @Parameter(hidden = true) HttpServletRequest servletRequest);

  @Operation(summary = "비회원 예약 취소", description = "대기 상태의 비회원 예약을 취소합니다. 조회와 같은 확인과 실패 횟수 제한을 거칩니다.")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "예약 취소 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "404", description = "일치하는 비회원 예약 없음"),
          @ApiResponse(responseCode = "409", description = "취소할 수 없는 예약 상태"),
          @ApiResponse(responseCode = "429", description = "조회 실패 횟수 초과")
      }
  )
  ReservationResponse cancelGuest(
      @Parameter(description = "예약 번호(예: 4821-7390)") String code,
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.GUEST_CANCEL)))
      GuestCancelRequest request,
      @Parameter(hidden = true) HttpServletRequest servletRequest);

  @Operation(summary = "내 예약 목록 조회", description = "로그인한 회원의 예약 목록을 조회합니다.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "조회 완료"),
          @ApiResponse(responseCode = "401", description = "로그인 필요")
      }
  )
  List<ReservationResponse> mine(Authentication authentication);

  @Operation(summary = "내 예약 상세 조회", description = "로그인한 회원의 예약 한 건을 조회합니다.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "조회 완료"),
          @ApiResponse(responseCode = "401", description = "로그인 필요"),
          @ApiResponse(responseCode = "404", description = "예약 없음")
      }
  )
  ReservationResponse mineOne(Authentication authentication,
      @Parameter(description = "예약 번호") Long id);

  @Operation(summary = "회원 예약 취소", description = "로그인한 회원의 대기 상태 예약을 취소합니다.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "예약 취소 완료"),
          @ApiResponse(responseCode = "401", description = "로그인 필요"),
          @ApiResponse(responseCode = "404", description = "예약 없음"),
          @ApiResponse(responseCode = "409", description = "취소할 수 없는 예약 상태")
      }
  )
  ReservationResponse cancel(Authentication authentication,
      @Parameter(description = "예약 번호") Long id);
}
