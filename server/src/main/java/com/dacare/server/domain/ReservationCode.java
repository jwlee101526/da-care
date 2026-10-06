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

  private final String value;

  private ReservationCode(String value) {
    this.value = value;
  }

  public static ReservationCode generate() {
    return new ReservationCode(String.format("%0" + LENGTH + "d", RANDOM.nextInt(BOUND)));
  }

  /**
   * 고객이 입력한 값을 읽는다. 하이픈과 공백은 무시한다.
   *
   * @return 숫자 8자리가 아니면 빈 값
   */
  public static Optional<ReservationCode> parse(String input) {
    if (input == null) {
      return Optional.empty();
    }
    String digits = input.replaceAll("[\\s-]", "");
    return DIGITS.matcher(digits).matches() ? Optional.of(new ReservationCode(digits))
        : Optional.empty();
  }

  /**
   * DB에 저장된 값을 다시 읽는다. 저장할 때 이미 검증했으므로 형식을 다시 확인하지 않는다.
   */
  static ReservationCode restore(String stored) {
    return new ReservationCode(stored);
  }

  /**
   * 저장 형식(숫자 8자리). 예: 48217390
   */
  public String value() {
    return value;
  }

  /**
   * 사람이 읽는 형식. 48217390 → 4821-7390
   */
  public String format() {
    return value.substring(0, LENGTH / 2) + "-" + value.substring(LENGTH / 2);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ReservationCode other && value.equals(other.value);
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
