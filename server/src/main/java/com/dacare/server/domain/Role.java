package com.dacare.server.domain;

public enum Role {
  CUSTOMER, ADMIN;

  /** Spring Security 권한 이름. {@code hasRole}이 비교하는 ROLE_ 접두어를 붙인다. */
  public String authority() {
    return "ROLE_" + name();
  }

  /**
   * 토큰 등 외부 값에서 역할을 읽는다.
   *
   * @throws IllegalArgumentException 값이 없거나 알 수 없는 역할일 때
   */
  public static Role from(String value) {
    if (value == null) {
      throw new IllegalArgumentException("역할 값이 없습니다.");
    }
    return valueOf(value);
  }
}
