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
    @PostMapping ReservationResponse create(Authentication authentication, @Valid @RequestBody ReservationRequest request) { return ReservationResponse.from(service.create(authentication.getName(), request.deviceType(), request.symptomDescription(), request.visitAddress(), request.preferredAt())); }
    @GetMapping("/me") List<ReservationResponse> mine(Authentication authentication) { return service.mine(authentication.getName()).stream().map(ReservationResponse::from).toList(); }
    record ReservationRequest(@NotBlank String deviceType, @NotBlank @Size(max = 2000) String symptomDescription, @NotBlank String visitAddress, @NotNull LocalDateTime preferredAt) {}
    public record ReservationResponse(Long id, String deviceType, String symptomDescription, String visitAddress, LocalDateTime preferredAt, LocalDateTime confirmedAt, String status, String engineerName) { static ReservationResponse from(Reservation reservation) { return new ReservationResponse(reservation.getId(), reservation.getDeviceType(), reservation.getSymptomDescription(), reservation.getVisitAddress(), reservation.getPreferredAt(), reservation.getConfirmedAt(), reservation.getStatus().name(), reservation.getEngineer() == null ? null : reservation.getEngineer().getName()); } }
}
