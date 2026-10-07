package com.dacare.server.domain;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.regex.Pattern;

/**
 * 전화번호 값 객체. Google libphonenumber로 국가별 규칙에 맞춰 검증하고 E.164 형식(예: +821012345678)으로 저장·비교한다.
 *
 * <p>국가번호 없이 입력한 번호는 {@link #DEFAULT_REGION}(한국) 번호로 해석한다. 해외 번호는 +국가번호로 입력하면
 * 그대로 받는다.
 */
public final class PhoneNumber {

  public static final String DEFAULT_REGION = "KR";

  private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();
  // libphonenumber는 영문자를 숫자로 바꿔(1-800-FLOWERS 등) 해석하므로, 숫자와 구분 기호만 허용한다.
  private static final Pattern INPUT = Pattern.compile("^\\+?[0-9\\s().-]+$");
  private static final String INVALID = "올바른 전화번호가 아닙니다.";
  private static final String INVALID_MOBILE = "올바른 휴대전화 번호가 아닙니다.";

  private final String value;

  private PhoneNumber(String value) {
    this.value = value;
  }

  /**
   * 사용자 입력을 해석해 E.164 형식으로 정규화한다.
   *
   * @throws IllegalArgumentException 형식이 올바르지 않거나 존재할 수 없는 번호일 때
   */
  public static PhoneNumber of(String input) {
    if (input == null || !INPUT.matcher(input.strip()).matches()) {
      throw new IllegalArgumentException(INVALID);
    }
    try {
      Phonenumber.PhoneNumber parsed = UTIL.parse(input, DEFAULT_REGION);
      if (!UTIL.isValidNumber(parsed)) {
        throw new IllegalArgumentException(INVALID);
      }
      return new PhoneNumber(UTIL.format(parsed, PhoneNumberFormat.E164));
    } catch (NumberParseException e) {
      throw new IllegalArgumentException(INVALID, e);
    }
  }

  /**
   * 휴대전화 번호만 받는다. 일정 안내 문자를 보내야 하는 연락처에 쓴다.
   *
   * @throws IllegalArgumentException 올바른 번호가 아니거나 휴대전화 번호가 아닐 때
   */
  public static PhoneNumber ofMobile(String input) {
    PhoneNumber phone = of(input);
    if (!phone.isMobile()) {
      throw new IllegalArgumentException(INVALID_MOBILE);
    }
    return phone;
  }

  /**
   * 값이 없으면 null을 돌려준다. 선택 입력 필드에 쓴다.
   */
  public static PhoneNumber ofNullableMobile(String input) {
    return input == null || input.isBlank() ? null : ofMobile(input);
  }

  public static boolean isValid(String input) {
    try {
      of(input);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static boolean isValidMobile(String input) {
    try {
      ofMobile(input);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * DB에 저장된 값을 복원한다. 저장 값은 마이그레이션으로 정규화되어 있으므로 다시 검증하지 않는다.
   */
  static PhoneNumber restore(String stored) {
    return new PhoneNumber(stored);
  }

  /**
   * E.164 형식 값. 저장과 비교에 쓴다.
   */
  public String value() {
    return value;
  }

  /**
   * 국가번호(한국은 82).
   */
  public int countryCode() {
    return parsed().getCountryCode();
  }

  /**
   * 휴대전화 번호인지. 미국처럼 유선과 휴대전화 번호 체계가 같은 나라의 번호도 포함한다.
   */
  public boolean isMobile() {
    PhoneNumberType type = UTIL.getNumberType(parsed());
    return type == PhoneNumberType.MOBILE || type == PhoneNumberType.FIXED_LINE_OR_MOBILE;
  }

  public boolean isDomestic() {
    return countryCode() == UTIL.getCountryCodeForRegion(DEFAULT_REGION);
  }

  /**
   * 국가번호와 국내 접두어(한국의 0)를 뺀 번호. 예: 1012345678
   */
  public String nationalSignificantNumber() {
    return UTIL.getNationalSignificantNumber(parsed());
  }

  /**
   * 국내에서 거는 번호를 숫자만 남긴 형태. 예: 01012345678
   */
  public String nationalDialingNumber() {
    return UTIL.format(parsed(), PhoneNumberFormat.NATIONAL).replaceAll("[^0-9]", "");
  }

  /**
   * 사람이 읽는 형식. 국내 번호는 010-1234-5678, 해외 번호는 +1 201-555-0123처럼 표시한다.
   */
  public String format() {
    try {
      return UTIL.format(parsed(),
          isDomestic() ? PhoneNumberFormat.NATIONAL : PhoneNumberFormat.INTERNATIONAL);
    } catch (IllegalStateException e) {
      return value;
    }
  }

  private Phonenumber.PhoneNumber parsed() {
    try {
      return UTIL.parse(value, DEFAULT_REGION);
    } catch (NumberParseException e) {
      throw new IllegalStateException("저장된 전화번호를 해석할 수 없습니다: " + value, e);
    }
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PhoneNumber other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
