package com.dacare.server.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ConfirmationRequest(@NotNull Long engineerId, @NotNull LocalDateTime confirmedAt) {

}
