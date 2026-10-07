package com.dacare.server.api.dto.request;

import com.dacare.server.service.diagnosis.DiagnosisService.ConversationTurn;
import com.dacare.server.service.diagnosis.DiagnosisService.Speaker;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConversationTurnRequest(@NotNull Speaker role,
                                      @NotBlank @Size(max = 4000) String text) {

  ConversationTurn toTurn() {
    return new ConversationTurn(role, text);
  }
}
