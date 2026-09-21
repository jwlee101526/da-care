package com.dacare.server.api;

import com.dacare.server.domain.Engineer;
import com.dacare.server.api.docs.AdminApiDocs;
import com.dacare.server.repository.EngineerRepository;
import com.dacare.server.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
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

  private final EngineerRepository engineers;
  private final ReservationService reservations;

  public AdminController(EngineerRepository engineers, ReservationService reservations) {
    this.engineers = engineers;
    this.reservations = reservations;
  }

  @GetMapping("/engineers")
  public List<EngineerResponse> engineers() {
    return engineers.findAll().stream().map(EngineerResponse::from).toList();
  }

  @PostMapping("/engineers")
  public EngineerResponse create(@Valid @RequestBody EngineerRequest request) {
    return EngineerResponse.from(engineers.save(
        new Engineer(request.name(), request.phone(), request.specialty(), request.region())));
  }

  @PutMapping("/engineers/{id}")
  public EngineerResponse update(@PathVariable Long id, @Valid @RequestBody EngineerRequest request) {
    Engineer engineer = engineers.findById(id)
        .orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다."));
    engineer.update(request.name(), request.phone(), request.specialty(), request.region());
    return EngineerResponse.from(engineers.save(engineer));
  }

  @DeleteMapping("/engineers/{id}")
  public void delete(@PathVariable Long id) {
    engineers.deleteById(id);
  }

  @GetMapping("/reservations")
  public List<ReservationController.ReservationResponse> reservations() {
    return reservations.all().stream().map(ReservationController.ReservationResponse::from)
        .toList();
  }

  @PatchMapping("/reservations/{id}/confirmation")
  public ReservationController.ReservationResponse confirm(@PathVariable Long id,
      @Valid @RequestBody ConfirmationRequest request) {
    return ReservationController.ReservationResponse.from(
        reservations.confirm(id, request.engineerId(), request.confirmedAt()));
  }

  @PatchMapping("/reservations/{id}/complete")
  public ReservationController.ReservationResponse complete(@PathVariable Long id) {
    return ReservationController.ReservationResponse.from(reservations.complete(id));
  }

  @PatchMapping("/reservations/{id}/cancel")
  public ReservationController.ReservationResponse cancel(@PathVariable Long id) {
    return ReservationController.ReservationResponse.from(reservations.cancelByAdmin(id));
  }

  public record EngineerRequest(@NotBlank String name, @Pattern(regexp = "^[0-9-]{9,13}$") String phone,
                         @NotBlank String specialty, @NotBlank String region) {

  }

  public record ConfirmationRequest(@NotNull Long engineerId, @NotNull LocalDateTime confirmedAt) {

  }

  public record EngineerResponse(Long id, String name, String phone, String specialty, String region) {

    static EngineerResponse from(Engineer engineer) {
      return new EngineerResponse(engineer.getId(), engineer.getName(), engineer.getPhone(),
          engineer.getSpecialty(), engineer.getRegion());
    }
  }
}
