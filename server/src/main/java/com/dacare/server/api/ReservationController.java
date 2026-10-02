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
                                 @NotBlank @ValidPhoneNumber(mobile = true) String contactPhone) {

    ReservationDraft toDraft() {
      return new ReservationDraft(deviceType, symptomDescription, visitAddress, preferredAt,
          contactName, PhoneNumber.ofMobile(contactPhone));
    }
  }

  public record GuestLookupRequest(@NotBlank @Size(max = 20) String reservationCode,
                            @NotBlank @ValidPhoneNumber String contactPhone) {

  }

  public record GuestCancelRequest(@NotBlank @ValidPhoneNumber String contactPhone) {

  }

  /**
   * @param id   회원·관리자 API에서만 쓰는 내부 번호. 순번이라 전체 예약 건수가 드러나므로 비회원 응답에서는 비운다.
   * @param code 고객에게 보여주는 예약 번호(숫자 8자리)
   */
  public record ReservationResponse(Long id, String code, String deviceType,
                                    String symptomDescription, String visitAddress,
                                    LocalDateTime preferredAt, LocalDateTime confirmedAt,
                                    String status, String engineerName, String contactName,
                                    String contactPhone) {

    static ReservationResponse from(Reservation reservation) {
      return of(reservation, reservation.getId(), reservation.getVisitAddress(),
          reservation.getContactName(),
          reservation.getContactPhone() == null ? null : reservation.getContactPhone().value());
    }

    /** 비회원 예약 접수 직후 응답. 방금 입력한 본인에게 돌려주는 것이므로 개인정보를 가리지 않는다. */
    static ReservationResponse forGuest(Reservation reservation) {
      return of(reservation, null, reservation.getVisitAddress(), reservation.getContactName(),
          reservation.getContactPhone().value());
    }

    /** 비회원 예약 조회·취소 응답. 이름·연락처·상세 주소를 가리며, 연락처는 표시용 문자열이다. */
    static ReservationResponse maskedForGuest(Reservation reservation) {
      return of(reservation, null, PersonalDataMask.address(reservation.getVisitAddress()),
          PersonalDataMask.name(reservation.getContactName()),
          PersonalDataMask.phone(reservation.getContactPhone()));
    }

    private static ReservationResponse of(Reservation reservation, Long id, String visitAddress,
        String contactName, String contactPhone) {
      return new ReservationResponse(id, reservation.getCode(), reservation.getDeviceType(),
          reservation.getSymptomDescription(), visitAddress, reservation.getPreferredAt(),
          reservation.getConfirmedAt(), reservation.getStatus().name(),
          reservation.getEngineer() == null ? null : reservation.getEngineer().getName(),
          contactName, contactPhone);
    }
  }
}
