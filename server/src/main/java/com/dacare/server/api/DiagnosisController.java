package com.dacare.server.api;
import com.dacare.server.service.DiagnosisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping(path = "/api/diagnosis", version = "1")
public class DiagnosisController {
    private final DiagnosisService service; public DiagnosisController(DiagnosisService service) { this.service = service; }
    @PostMapping("/chat") DiagnosisService.DiagnosisResult chat(@Valid @RequestBody QuestionRequest request) { return service.diagnose(request.question()); }
    record QuestionRequest(@NotBlank String question) {}
}
