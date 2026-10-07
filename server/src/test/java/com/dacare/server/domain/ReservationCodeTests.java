package com.dacare.server.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReservationCodeTests {

  @Test
  void generatesEightDigits() {
    for (int i = 0; i < 1000; i++) {
      assertThat(ReservationCode.generate().value()).matches("\\d{8}");
    }
  }

  @Test
  void formatsInGroupsOfFour() {
    assertThat(ReservationCode.parse("04821739").orElseThrow().format()).isEqualTo("0482-1739");
  }

  @Test
  void parsesIgnoringHyphenAndSpaces() {
    assertThat(ReservationCode.parse("4821-7390")).map(ReservationCode::value).contains("48217390");
    assertThat(ReservationCode.parse(" 4821 7390 ")).contains(
        ReservationCode.parse("48217390").orElseThrow());
  }

  @Test
  void rejectsMalformedInput() {
    assertThat(ReservationCode.parse(null)).isEmpty();
    assertThat(ReservationCode.parse("128")).isEmpty();
    assertThat(ReservationCode.parse("482173901")).isEmpty();
    assertThat(ReservationCode.parse("4821-739O")).isEmpty();
  }
}
