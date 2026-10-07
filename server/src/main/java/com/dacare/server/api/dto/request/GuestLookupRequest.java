package com.dacare.server.api.dto.request;

import com.dacare.server.api.validation.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestLookupRequest(@NotBlank @Size(max = 20) String reservationCode,
                                 @NotBlank @ValidPhoneNumber String contactPhone) {

}
