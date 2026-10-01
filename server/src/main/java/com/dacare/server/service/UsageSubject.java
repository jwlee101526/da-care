package com.dacare.server.service;

/**
 * AI 상담 사용량을 따로 세는 단위. 로그인 사용자는 계정, 비로그인 사용자는 IP(해시)로 구분한다.
 *
 * @param id 계정 이메일 또는 IP 해시
 */
public record UsageSubject(Type type, String id) {

  public enum Type {CUSTOMER, ADMIN, GUEST}

  public static UsageSubject customer(String email) {
    return new UsageSubject(Type.CUSTOMER, email);
  }

  public static UsageSubject admin(String email) {
    return new UsageSubject(Type.ADMIN, email);
  }

  public static UsageSubject guest(String ipHash) {
    return new UsageSubject(Type.GUEST, ipHash);
  }

  public boolean isGuest() {
    return type == Type.GUEST;
  }

  /**
   * 사용량 테이블의 subject 값. 계정은 역할이 바뀌어도 같은 집계를 쓰도록 역할을 넣지 않는다.
   */
  String storageKey() {
    return (isGuest() ? "ip:" : "user:") + id;
  }
}
