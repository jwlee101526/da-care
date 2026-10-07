package com.dacare.server.api.dto.request;

import com.dacare.server.api.validation.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;

public record GuestCancelRequest(@NotBlank @ValidPhoneNumber String contactPhone) {

}
