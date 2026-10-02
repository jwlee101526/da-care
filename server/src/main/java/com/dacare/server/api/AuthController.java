package com.dacare.server.api;

import com.dacare.server.service.AuthService;
import com.dacare.server.api.docs.AuthApiDocs;
import com.dacare.server.api.validation.ValidPhoneNumber;
import com.dacare.server.domain.PhoneNumber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth", version = "1")
public class AuthController implements AuthApiDocs {

  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/signup")
  public TokenResponse signup(@Valid @RequestBody SignupRequest request) {
    return new TokenResponse(
        service.signup(request.email(), request.password(), request.name(), PhoneNumber.ofMobile(request.phone()),
            request.address()));
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return new TokenResponse(service.login(request.email(), request.password()));
  }

  public record SignupRequest(@NotBlank @Email String email, @Size(min = 8) String password,
                       @NotBlank String name, @NotBlank @ValidPhoneNumber(mobile = true) String phone,
                       @NotBlank String address) {

  }

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

  }

  public record TokenResponse(String accessToken) {

  }
}
