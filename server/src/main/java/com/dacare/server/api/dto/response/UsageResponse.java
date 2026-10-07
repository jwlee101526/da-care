package com.dacare.server.api.dto.response;

import com.dacare.server.service.usage.ApiUsageService.Usage;

public record UsageResponse(int used, int limit, int remaining) {

  static UsageResponse from(Usage usage) {
    return new UsageResponse(usage.used(), usage.limit(), usage.remaining());
  }
}
