package com.dacare.server.api.controller;

import com.dacare.server.api.docs.ReservationApiDocs;
import com.dacare.server.api.dto.request.GuestCancelRequest;
import com.dacare.server.api.dto.request.GuestLookupRequest;
import com.dacare.server.api.dto.request.GuestReservationRequest;
import com.dacare.server.api.dto.request.ReservationRequest;
import com.dacare.server.api.dto.response.ReservationResponse;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.service.ReservationService;
import com.dacare.server.web.UsageSubjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/reservations", version = "1")
public class ReservationController implements ReservationApiDocs {

  private final ReservationService service;
  private final UsageSubjectResolver subjects;

  public ReservationController(ReservationService service, UsageSubjectResolver subjects) {
    this.service = service;
    this.subjects = subjects;
  }

  @PostMapping
  public ReservationResponse create(Authentication authentication,
      @Valid @RequestBody ReservationRequest request) {
    return ReservationResponse.from(service.create(authentication.getName(), request.toDraft()));
  }

  @PostMapping("/guest")
  public ReservationResponse createGuest(@Valid @RequestBody GuestReservationRequest request) {
    return ReservationResponse.forGuest(service.createGuest(request.toDraft()));
  }

  @PostMapping("/guest/lookup")
  public ReservationResponse lookupGuest(@Valid @RequestBody GuestLookupRequest request,
      HttpServletRequest servletRequest) {
    return ReservationResponse.maskedForGuest(
        service.findGuest(request.reservationCode(), PhoneNumber.of(request.contactPhone()),
            subjects.clientIpHash(servletRequest)));
  }

  @PatchMapping("/guest/{code}/cancel")
  public ReservationResponse cancelGuest(@PathVariable String code,
      @Valid @RequestBody GuestCancelRequest request, HttpServletRequest servletRequest) {
    return ReservationResponse.maskedForGuest(
        service.cancelGuest(code, PhoneNumber.of(request.contactPhone()),
            subjects.clientIpHash(servletRequest)));
  }

  @GetMapping("/me")
  public List<ReservationResponse> mine(Authentication authentication) {
    return service.mine(authentication.getName()).stream().map(ReservationResponse::from).toList();
  }

  @GetMapping("/{id}")
  public ReservationResponse mineOne(Authentication authentication, @PathVariable Long id) {
    return ReservationResponse.from(service.mineOne(authentication.getName(), id));
  }

  @PatchMapping("/{id}/cancel")
  public ReservationResponse cancel(Authentication authentication, @PathVariable Long id) {
    return ReservationResponse.from(service.cancel(authentication.getName(), id));
  }
}
