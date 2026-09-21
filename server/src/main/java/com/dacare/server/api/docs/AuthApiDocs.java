package com.dacare.server.api.docs;

import com.dacare.server.api.AuthController.LoginRequest;
import com.dacare.server.api.AuthController.SignupRequest;
import com.dacare.server.api.AuthController.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증", description = "회원 가입과 로그인")
public interface AuthApiDocs {

  @Operation(summary = "회원 가입", description = "회원 계정을 생성하고 JWT 액세스 토큰을 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "회원 가입 완료"),
      @ApiResponse(responseCode = "400", description = "입력값 오류")
  })
  TokenResponse signup(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
      content = @Content(schema = @Schema(implementation = SignupRequest.class), examples = @ExampleObject("""
          {"email":"user@example.com","password":"password1234","name":"홍길동","phone":"010-1234-5678","address":"서울특별시 강남구 테헤란로 1"}
          """))) SignupRequest request);

  @Operation(summary = "로그인", description = "이메일과 비밀번호를 검증하고 JWT 액세스 토큰을 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "로그인 완료"),
      @ApiResponse(responseCode = "400", description = "입력값 오류")
  })
  TokenResponse login(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
      content = @Content(schema = @Schema(implementation = LoginRequest.class), examples = @ExampleObject("""
          {"email":"user@example.com","password":"password1234"}
          """))) LoginRequest request);
}
