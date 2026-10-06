package com.dacare.server.domain;

/**
 * 수리 예약 화면에서 고르는 기기 분류 12종. 값은 웹 클라이언트가 주고받는 소문자 이름 그대로 저장한다. 목록에 없는 기기는 긴급 출장 A/S로 접수한다.
 */
public enum DeviceType {
  smartphone("스마트폰 · 태블릿"),
  computer("데스크탑 · PC / 노트북"),
  tv("스마트 TV"),
  console("게임 콘솔"),
  aircon("에어컨"),
  washing("세탁기 · 건조기"),
  fridge("냉장고"),
  microwave("전자레인지 · 인덕션"),
  cleaner("청소기"),
  internet("네트워크 · 공유기"),
  audio("음향 기기 · 오디오"),
  repair("긴급 출장 A/S");

  private final String label;

  DeviceType(String label) {
    this.label = label;
  }

  /**
   * 운영 알림 등에 쓰는 한국어 이름.
   */
  public String label() {
    return label;
  }
}
