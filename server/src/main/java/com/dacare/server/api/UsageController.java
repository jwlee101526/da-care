package com.dacare.server.api;

import com.dacare.server.api.docs.UsageApiDocs;
import com.dacare.server.service.ApiUsageService;
import com.dacare.server.service.ApiUsageService.DailyUsage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/usage", version = "1")
public class UsageController implements UsageApiDocs {

  private final ApiUsageService service;

  public UsageController(ApiUsageService service) {
    this.service = service;
  }

  @GetMapping
  public DailyUsage today() {
    return service.todayUsage();
  }
}
