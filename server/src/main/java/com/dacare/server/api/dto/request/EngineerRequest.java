package com.dacare.server.api.dto.request;

import com.dacare.server.api.validation.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;

public record EngineerRequest(@NotBlank String name,
                              @NotBlank @ValidPhoneNumber(mobile = true) String phone,
                              @NotBlank String specialty, @NotBlank String region) {

}
