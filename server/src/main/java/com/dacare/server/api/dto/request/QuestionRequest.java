package com.dacare.server.api.dto.request;

import com.dacare.server.service.diagnosis.DiagnosisService.ConversationTurn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuestionRequest(@NotBlank @Size(max = 2000) String question,
                              @Size(max = 12) List<@NotNull @Valid ConversationTurnRequest> history) {

  public List<ConversationTurn> turns() {
    return history == null ? List.of()
        : history.stream().map(ConversationTurnRequest::toTurn).toList();
  }
}
