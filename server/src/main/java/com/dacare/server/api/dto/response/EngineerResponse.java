package com.dacare.server.api.dto.response;

import com.dacare.server.domain.Engineer;

public record EngineerResponse(Long id, String name, String phone, String specialty,
                               String region) {

  public static EngineerResponse from(Engineer engineer) {
    return new EngineerResponse(engineer.getId(), engineer.getName(), engineer.getPhone().value(),
        engineer.getSpecialty(), engineer.getRegion());
  }
}
