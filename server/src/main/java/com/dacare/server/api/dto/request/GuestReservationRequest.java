package com.dacare.server.api.dto.request;

import com.dacare.server.api.validation.ValidPhoneNumber;
import com.dacare.server.domain.DeviceType;
import com.dacare.server.domain.PhoneNumber;
import com.dacare.server.service.ReservationDraft;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record GuestReservationRequest(@NotNull DeviceType deviceType,
                                      @NotBlank @Size(max = 2000) String symptomDescription,
                                      @NotBlank @Size(max = 200) String visitAddress,
                                      @NotNull LocalDateTime preferredAt,
                                      @NotBlank @Size(min = 1, max = 50) String contactName,
                                      @NotBlank @ValidPhoneNumber(mobile = true) String contactPhone) {

  public ReservationDraft toDraft() {
    return new ReservationDraft(deviceType, symptomDescription, visitAddress, preferredAt,
        contactName, PhoneNumber.ofMobile(contactPhone));
  }
}
