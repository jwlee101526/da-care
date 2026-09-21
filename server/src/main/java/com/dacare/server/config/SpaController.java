package com.dacare.server.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

  @GetMapping({"/", "/en", "/ko", "/reserve", "/reservations", "/reservations/new",
      "/order", "/login", "/signup", "/admin", "/en/reserve", "/en/reservations",
      "/en/reservations/new", "/en/login", "/en/signup"})
  public String index() {
    return "forward:/index.html";
  }
}
