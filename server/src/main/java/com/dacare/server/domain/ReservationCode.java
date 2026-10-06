package com.dacare.server.domain;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 고객에게 보여주는 예약 번호(무작위 숫자 8자리, 예: 48217390). 순번인 id는 다른 예약을 짐작하거나 전체 예약 건수를 알아낼 수 있어 무작위 값을 따로 쓴다.
 * 숫자만 쓰는 것은 휴대전화 숫자 자판으로 바로 입력할 수 있게 하기 위해서다.
 * <p>
 * 화면에서는 4821-7390처럼 4자리씩 끊어 보여주고, 저장과 비교는 숫자만으로 한다. 발급한 번호는 예약이 끝나도 다시 쓰지 않는다.
 */
public final class ReservationCode {

  public static final int LENGTH = 8;
  private static final int BOUND = 100_000_000;
  private static final Pattern DIGITS = Pattern.compile("\\d{" + LENGTH + "}");
  private static final SecureRandom RANDOM = new SecureRandom();

  private ReservationCode() {
  }

  public static String generate() {
    return String.format("%0" + LENGTH + "d", RANDOM.nextInt(BOUND));
  }

  /**
   * 사람이 읽는 형식. 48217390 → 4821-7390
   */
  public static String format(String code) {
    return code.substring(0, LENGTH / 2) + "-" + code.substring(LENGTH / 2);
  }

  /**
   * 고객이 입력한 값을 저장 형식으로 바꾼다. 하이픈과 공백은 무시한다.
   *
   * @return 숫자 8자리가 아니면 빈 값
   */
  public static Optional<String> normalize(String input) {
    if (input == null) {
      return Optional.empty();
    }
    String digits = input.replaceAll("[\\s-]", "");
    return DIGITS.matcher(digits).matches() ? Optional.of(digits) : Optional.empty();
  }
}
