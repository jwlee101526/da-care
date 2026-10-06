package com.dacare.server.api.docs;

import com.dacare.server.api.AuthController.LoginRequest;
import com.dacare.server.api.AuthController.SignupRequest;
import com.dacare.server.api.AuthController.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "회원 가입과 로그인 API")
public interface AuthApiDocs {

  @Operation(summary = "회원 가입", description = "회원 계정을 생성하고 JWT 액세스 토큰을 반환합니다.")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "회원 가입 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
      }
  )
  TokenResponse signup(
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.SIGNUP)))
      SignupRequest request);

  @Operation(summary = "로그인", description = "이메일과 비밀번호를 검증하고 JWT 액세스 토큰을 반환합니다.")
  @ApiResponses(
      {
          @ApiResponse(responseCode = "200", description = "로그인 완료"),
          @ApiResponse(responseCode = "400", description = "입력값 오류"),
          @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
      }
  )
  TokenResponse login(
      @RequestBody(content = @Content(examples = @ExampleObject(ApiExamples.LOGIN)))
      LoginRequest request);
}
