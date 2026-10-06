package com.dacare.server.api.dto.request;

import com.dacare.server.api.validation.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(@NotBlank @Email String email, @Size(min = 8) String password,
                            @NotBlank String name,
                            @NotBlank @ValidPhoneNumber(mobile = true) String phone,
                            @NotBlank String address) {

}
