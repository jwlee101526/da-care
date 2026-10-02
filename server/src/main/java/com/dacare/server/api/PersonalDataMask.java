package com.dacare.server.api;

import com.dacare.server.domain.PhoneNumber;
import java.util.Arrays;

/**
 * 비회원 예약 조회 응답에 내보내는 개인정보를 일부 가린다. 예약 번호와 휴대전화 번호를 맞힌 사람이 본인이 아니더라도 이름·연락처·상세 주소가
 * 그대로 드러나지 않게 하기 위해서다.
 */
final class PersonalDataMask {

  private static final char MASK = '*';
  private static final int VISIBLE_PHONE_DIGITS = 4;
  private static final int VISIBLE_ADDRESS_WORDS = 2;

  private PersonalDataMask() {
  }

  /** 홍길동 → 홍*동, 김철 → 김*, 이 → * */
  static String name(String name) {
    if (name == null || name.isBlank()) {
      return name;
    }
    String trimmed = name.strip();
    int length = trimmed.codePointCount(0, trimmed.length());
    if (length == 1) {
      return String.valueOf(MASK);
    }
    int[] chars = trimmed.codePoints().toArray();
    StringBuilder masked = new StringBuilder().appendCodePoint(chars[0]);
    masked.append(String.valueOf(MASK).repeat(length == 2 ? 1 : length - 2));
    if (length > 2) {
      masked.appendCodePoint(chars[length - 1]);
    }
    return masked.toString();
  }

  /** 첫 묶음과 마지막 4자리만 남긴다. 010-1234-5678 → 010-****-5678, +1 201-555-0123 → +1 ***-***-0123 */
  static String phone(PhoneNumber phone) {
    if (phone == null) {
      return null;
    }
    String formatted = phone.format();
    int firstSeparator = indexOfSeparator(formatted);
    int digitsLeft = (int) formatted.chars().filter(Character::isDigit).count();
    StringBuilder masked = new StringBuilder(formatted.length());
    for (int i = 0; i < formatted.length(); i++) {
      char c = formatted.charAt(i);
      if (Character.isDigit(c)) {
        boolean visible = i < firstSeparator || digitsLeft <= VISIBLE_PHONE_DIGITS;
        masked.append(visible ? c : MASK);
        digitsLeft--;
      } else {
        masked.append(c);
      }
    }
    return masked.toString();
  }

  /** 시·도와 시·군·구까지만 남긴다. 서울특별시 강남구 테헤란로 1 → 서울특별시 강남구 *** */
  static String address(String address) {
    if (address == null || address.isBlank()) {
      return address;
    }
    String[] words = address.strip().split("\\s+");
    int visible = Math.min(VISIBLE_ADDRESS_WORDS, words.length - 1);
    if (visible <= 0) {
      return "***";
    }
    return String.join(" ", Arrays.copyOf(words, visible)) + " ***";
  }

  private static int indexOfSeparator(String formatted) {
    for (int i = 0; i < formatted.length(); i++) {
      char c = formatted.charAt(i);
      if (c == '-' || c == ' ') {
        return i;
      }
    }
    return 0;
  }
}
