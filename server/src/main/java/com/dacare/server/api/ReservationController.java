package com.dacare.server.api;

import com.dacare.server.api.docs.ReservationApiDocs;
import com.dacare.server.api.validation.ValidPhoneNumber;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.domain.Reservation;
import com.dacare.server.service.ReservationDraft;
import com.dacare.server.service.ReservationService;
import com.dacare.server.web.UsageSubjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
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

  private static final String GUEST_PASSWORD_PATTERN = "\\d{4}";
  private static final String GUEST_PASSWORD_MESSAGE = "조회용 비밀번호는 숫자 4자리입니다.";

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
    return ReservationResponse.from(
        service.createGuest(request.toDraft(), request.guestPassword()));
  }

  @PostMapping("/guest/lookup")
  public ReservationResponse lookupGuest(@Valid @RequestBody GuestLookupRequest request,
      HttpServletRequest servletRequest) {
    return ReservationResponse.from(
        service.findGuest(request.reservationId(), PhoneNumber.of(request.contactPhone()),
            request.guestPassword(), subjects.clientIpHash(servletRequest)));
  }

  @PatchMapping("/guest/{id}/cancel")
  public ReservationResponse cancelGuest(@PathVariable Long id,
      @Valid @RequestBody GuestCancelRequest request, HttpServletRequest servletRequest) {
    return ReservationResponse.from(
        service.cancelGuest(id, PhoneNumber.of(request.contactPhone()), request.guestPassword(),
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

  public record ReservationRequest(@NotBlank @Size(max = 50) String deviceType,
                            @NotBlank @Size(max = 2000) String symptomDescription,
                            @NotBlank @Size(max = 200) String visitAddress,
                            @NotNull LocalDateTime preferredAt,
                            @Size(min = 1, max = 50) String contactName,
                            @ValidPhoneNumber(mobile = true) String contactPhone) {

    ReservationDraft toDraft() {
      return new ReservationDraft(deviceType, symptomDescription, visitAddress, preferredAt,
          contactName, PhoneNumber.ofNullableMobile(contactPhone));
    }
  }

  public record GuestReservationRequest(@NotBlank @Size(max = 50) String deviceType,
                                 @NotBlank @Size(max = 2000) String symptomDescription,
                                 @NotBlank @Size(max = 200) String visitAddress,
                                 @NotNull LocalDateTime preferredAt,
                                 @NotBlank @Size(min = 1, max = 50) String contactName,
                                 @NotBlank @ValidPhoneNumber(mobile = true) String contactPhone,
                                 @NotBlank @Pattern(regexp = GUEST_PASSWORD_PATTERN,
                                     message = GUEST_PASSWORD_MESSAGE) String guestPassword) {

    ReservationDraft toDraft() {
      return new ReservationDraft(deviceType, symptomDescription, visitAddress, preferredAt,
          contactName, PhoneNumber.ofMobile(contactPhone));
    }
  }

  public record GuestLookupRequest(@NotNull Long reservationId,
                            @NotBlank @ValidPhoneNumber String contactPhone,
                            @NotBlank @Pattern(regexp = GUEST_PASSWORD_PATTERN,
                                message = GUEST_PASSWORD_MESSAGE) String guestPassword) {

  }

  public record GuestCancelRequest(@NotBlank @ValidPhoneNumber String contactPhone,
                            @NotBlank @Pattern(regexp = GUEST_PASSWORD_PATTERN,
                                message = GUEST_PASSWORD_MESSAGE) String guestPassword) {

  }

  public record ReservationResponse(Long id, String deviceType, String symptomDescription,
                                    String visitAddress, LocalDateTime preferredAt,
                                    LocalDateTime confirmedAt, String status, String engineerName,
                                    String contactName, String contactPhone) {

    static ReservationResponse from(Reservation reservation) {
      return new ReservationResponse(reservation.getId(), reservation.getDeviceType(),
          reservation.getSymptomDescription(), reservation.getVisitAddress(),
          reservation.getPreferredAt(), reservation.getConfirmedAt(),
          reservation.getStatus().name(),
          reservation.getEngineer() == null ? null : reservation.getEngineer().getName(),
          reservation.getContactName(),
          reservation.getContactPhone() == null ? null : reservation.getContactPhone().value());
    }
  }
}
