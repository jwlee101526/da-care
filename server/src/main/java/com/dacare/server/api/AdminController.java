package com.dacare.server.api;

import com.dacare.server.api.docs.AdminApiDocs;
import com.dacare.server.api.validation.ValidPhoneNumber;
import com.dacare.server.domain.Engineer;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.service.ApiUsageService;
import com.dacare.server.service.ApiUsageService.TotalUsage;
import com.dacare.server.service.EngineerService;
import com.dacare.server.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/admin", version = "1")
public class AdminController implements AdminApiDocs {

  private final EngineerService engineers;
  private final ReservationService reservations;
  private final ApiUsageService usage;

  public AdminController(EngineerService engineers, ReservationService reservations,
      ApiUsageService usage) {
    this.engineers = engineers;
    this.reservations = reservations;
    this.usage = usage;
  }

  @GetMapping("/usage")
  public TotalUsage usage() {
    return usage.totalUsage();
  }

  @GetMapping("/engineers")
  public List<EngineerResponse> engineers() {
    return engineers.all().stream().map(EngineerResponse::from).toList();
  }

  @PostMapping("/engineers")
  public EngineerResponse create(@Valid @RequestBody EngineerRequest request) {
    return EngineerResponse.from(
        engineers.create(request.name(), PhoneNumber.ofMobile(request.phone()),
            request.specialty(), request.region()));
  }

  @PutMapping("/engineers/{id}")
  public EngineerResponse update(@PathVariable Long id,
      @Valid @RequestBody EngineerRequest request) {
    return EngineerResponse.from(
        engineers.update(id, request.name(), PhoneNumber.ofMobile(request.phone()),
            request.specialty(), request.region()));
  }

  @DeleteMapping("/engineers/{id}")
  public void delete(@PathVariable Long id) {
    engineers.delete(id);
  }

  @GetMapping("/reservations")
  public List<ReservationResponse> reservations() {
    return reservations.all().stream().map(ReservationResponse::from).toList();
  }

  @PatchMapping("/reservations/{id}/confirmation")
  public ReservationResponse confirm(@PathVariable Long id,
      @Valid @RequestBody ConfirmationRequest request) {
    return ReservationResponse.from(
        reservations.confirm(id, request.engineerId(), request.confirmedAt()));
  }

  @PatchMapping("/reservations/{id}/complete")
  public ReservationResponse complete(@PathVariable Long id) {
    return ReservationResponse.from(reservations.complete(id));
  }

  @PatchMapping("/reservations/{id}/cancel")
  public ReservationResponse cancel(@PathVariable Long id) {
    return ReservationResponse.from(reservations.cancelByAdmin(id));
  }

  public record EngineerRequest(@NotBlank String name,
                                @NotBlank @ValidPhoneNumber(mobile = true) String phone,
                                @NotBlank String specialty, @NotBlank String region) {

  }

  public record ConfirmationRequest(@NotNull Long engineerId, @NotNull LocalDateTime confirmedAt) {

  }

  public record EngineerResponse(Long id, String name, String phone, String specialty,
                                 String region) {

    static EngineerResponse from(Engineer engineer) {
      return new EngineerResponse(engineer.getId(), engineer.getName(), engineer.getPhone().value(),
          engineer.getSpecialty(), engineer.getRegion());
    }
  }
}
