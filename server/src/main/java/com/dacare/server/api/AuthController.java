package com.dacare.server.api;

import com.dacare.server.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth", version = "1")
public class AuthController {

  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/signup")
  TokenResponse signup(@Valid @RequestBody SignupRequest request) {
    return new TokenResponse(
        service.signup(request.email(), request.password(), request.name(), request.phone(),
            request.address()));
  }

  @PostMapping("/login")
  TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return new TokenResponse(service.login(request.email(), request.password()));
  }

  record SignupRequest(@NotBlank @Email String email, @Size(min = 8) String password,
                       @NotBlank String name, @Pattern(regexp = "^[0-9-]{9,13}$") String phone,
                       @NotBlank String address) {

  }

  record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

  }

  record TokenResponse(String accessToken) {

  }
}
