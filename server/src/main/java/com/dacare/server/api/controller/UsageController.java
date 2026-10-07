package com.dacare.server.api.controller;

import com.dacare.server.api.docs.UsageApiDocs;
import com.dacare.server.api.dto.response.SubjectUsageResponse;
import com.dacare.server.service.usage.ApiUsageService;
import com.dacare.server.web.UsageSubjectResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/usage", version = "1")
public class UsageController implements UsageApiDocs {

  private final ApiUsageService service;
  private final UsageSubjectResolver subjects;

  public UsageController(ApiUsageService service, UsageSubjectResolver subjects) {
    this.service = service;
    this.subjects = subjects;
  }

  @GetMapping
  public SubjectUsageResponse mine(Authentication authentication, HttpServletRequest request) {
    return SubjectUsageResponse.from(
        service.diagnosisUsage(subjects.resolve(authentication, request)));
  }
}
