package com.dacare.server.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SPA 클라이언트 라우트를 새로고침하거나 직접 열어도 index.html을 받도록 포워딩한다.
 */
@Configuration
public class SpaForwardingConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    for (String path : SpaRoutes.PATHS) {
      registry.addViewController(path).setViewName("forward:/index.html");
    }
  }
}
