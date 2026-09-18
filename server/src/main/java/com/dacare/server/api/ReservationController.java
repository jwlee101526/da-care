package com.dacare.server.api;
import com.dacare.server.domain.Reservation;
import com.dacare.server.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping(path = "/api/reservations", version = "1")
public class ReservationController {
    private final ReservationService service; public ReservationController(ReservationService service) { this.service = service; }
    @PostMapping ReservationResponse create(Authentication authentication, @Valid @RequestBody ReservationRequest request) { return ReservationResponse.from(service.create(authentication.getName(), request.deviceType(), request.symptomDescription(), request.visitAddress(), request.preferredAt(), request.contactName(), request.contactPhone())); }
    @PostMapping("/guest") ReservationResponse createGuest(@Valid @RequestBody GuestReservationRequest request) { return ReservationResponse.from(service.createGuest(request.deviceType(), request.symptomDescription(), request.visitAddress(), request.preferredAt(), request.contactName(), request.contactPhone(), request.guestPassword())); }
    @PostMapping("/guest/lookup") ReservationResponse lookupGuest(@Valid @RequestBody GuestLookupRequest request) { return ReservationResponse.from(service.findGuest(request.reservationId(), request.contactPhone(), request.guestPassword())); }
    @PatchMapping("/guest/{id}/cancel") ReservationResponse cancelGuest(@PathVariable Long id, @Valid @RequestBody GuestCancelRequest request) { return ReservationResponse.from(service.cancelGuest(id, request.contactPhone(), request.guestPassword())); }
    @GetMapping("/me") List<ReservationResponse> mine(Authentication authentication) { return service.mine(authentication.getName()).stream().map(ReservationResponse::from).toList(); }
    @GetMapping("/{id}") ReservationResponse mineOne(Authentication authentication, @PathVariable Long id) { return ReservationResponse.from(service.mineOne(authentication.getName(), id)); }
    @PatchMapping("/{id}/cancel") ReservationResponse cancel(Authentication authentication, @PathVariable Long id) { return ReservationResponse.from(service.cancel(authentication.getName(), id)); }
    record ReservationRequest(@NotBlank @Size(max = 50) String deviceType, @NotBlank @Size(max = 2000) String symptomDescription, @NotBlank @Size(max = 200) String visitAddress, @NotNull LocalDateTime preferredAt, @Size(min = 1, max = 50) String contactName, @Pattern(regexp = "^[0-9-]{9,13}$") String contactPhone) {}
    record GuestReservationRequest(@NotBlank @Size(max = 50) String deviceType, @NotBlank @Size(max = 2000) String symptomDescription, @NotBlank @Size(max = 200) String visitAddress, @NotNull LocalDateTime preferredAt, @NotBlank @Size(min = 1, max = 50) String contactName, @NotBlank @Pattern(regexp = "^[0-9-]{9,13}$") String contactPhone, @Size(max = 20) String guestPassword) {}
    record GuestLookupRequest(@NotNull Long reservationId, @NotBlank @Pattern(regexp = "^[0-9-]{9,13}$") String contactPhone, String guestPassword) {}
    record GuestCancelRequest(@NotBlank @Pattern(regexp = "^[0-9-]{9,13}$") String contactPhone, String guestPassword) {}
    public record ReservationResponse(Long id, String deviceType, String symptomDescription, String visitAddress, LocalDateTime preferredAt, LocalDateTime confirmedAt, String status, String engineerName, String contactName, String contactPhone) { static ReservationResponse from(Reservation reservation) { return new ReservationResponse(reservation.getId(), reservation.getDeviceType(), reservation.getSymptomDescription(), reservation.getVisitAddress(), reservation.getPreferredAt(), reservation.getConfirmedAt(), reservation.getStatus().name(), reservation.getEngineer() == null ? null : reservation.getEngineer().getName(), reservation.getContactName(), reservation.getContactPhone()); } }
}
