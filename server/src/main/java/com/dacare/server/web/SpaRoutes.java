package com.dacare.server.web;

/**
 * 서버가 index.html로 포워딩하는 SPA 클라이언트 라우트. 라우팅과 보안 허용 목록이 함께 사용한다.
 */
public final class SpaRoutes {

  public static final String[] PATHS = {"/", "/en", "/ko", "/reserve", "/reservations",
      "/reservations/new", "/reservations/lookup", "/order", "/login", "/signup", "/admin",
      "/en/reserve", "/en/reservations", "/en/reservations/new", "/en/reservations/lookup",
      "/en/login", "/en/signup"};

  private SpaRoutes() {
  }
}
