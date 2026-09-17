package com.dacare.server.api;
import com.dacare.server.domain.*;
import com.dacare.server.repository.*;
import com.dacare.server.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping(path = "/api/admin", version = "1")
public class AdminController {
    private final EngineerRepository engineers; private final ReservationService reservations;
    public AdminController(EngineerRepository engineers, ReservationService reservations) { this.engineers = engineers; this.reservations = reservations; }
    @GetMapping("/engineers") List<EngineerResponse> engineers() { return engineers.findAll().stream().map(EngineerResponse::from).toList(); }
    @PostMapping("/engineers") EngineerResponse create(@Valid @RequestBody EngineerRequest request) { return EngineerResponse.from(engineers.save(new Engineer(request.name(), request.phone(), request.specialty(), request.region()))); }
    @PutMapping("/engineers/{id}") EngineerResponse update(@PathVariable Long id, @Valid @RequestBody EngineerRequest request) { Engineer engineer = engineers.findById(id).orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다.")); engineer.update(request.name(), request.phone(), request.specialty(), request.region()); return EngineerResponse.from(engineers.save(engineer)); }
    @DeleteMapping("/engineers/{id}") void delete(@PathVariable Long id) { engineers.deleteById(id); }
    @GetMapping("/reservations") List<ReservationController.ReservationResponse> reservations() { return reservations.all().stream().map(ReservationController.ReservationResponse::from).toList(); }
    @PatchMapping("/reservations/{id}/confirmation") ReservationController.ReservationResponse confirm(@PathVariable Long id, @Valid @RequestBody ConfirmationRequest request) { return ReservationController.ReservationResponse.from(reservations.confirm(id, request.engineerId(), request.confirmedAt())); }
    record EngineerRequest(@NotBlank String name, @Pattern(regexp = "^[0-9-]{9,13}$") String phone, @NotBlank String specialty, @NotBlank String region) {}
    record ConfirmationRequest(@NotNull Long engineerId, @NotNull LocalDateTime confirmedAt) {}
    record EngineerResponse(Long id, String name, String phone, String specialty, String region) { static EngineerResponse from(Engineer engineer) { return new EngineerResponse(engineer.getId(), engineer.getName(), engineer.getPhone(), engineer.getSpecialty(), engineer.getRegion()); } }
}
