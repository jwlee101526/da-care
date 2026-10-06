package com.dacare.server.api.controller;

import com.dacare.server.api.docs.AuthApiDocs;
import com.dacare.server.api.dto.request.LoginRequest;
import com.dacare.server.api.dto.request.SignupRequest;
import com.dacare.server.api.dto.response.TokenResponse;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.service.AuthService;
import jakarta.validation.Valid;
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
        service.signup(request.email(), request.password(), request.name(),
            PhoneNumber.ofMobile(request.phone()),
            request.address()));
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return new TokenResponse(service.login(request.email(), request.password()));
  }
}
