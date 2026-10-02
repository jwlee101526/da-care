package com.dacare.server.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReservationCodeTests {

  @Test
  void generatesEightDigits() {
    for (int i = 0; i < 1000; i++) {
      assertThat(ReservationCode.generate()).matches("\\d{8}");
    }
  }

  @Test
  void formatsInGroupsOfFour() {
    assertThat(ReservationCode.format("04821739")).isEqualTo("0482-1739");
  }

  @Test
  void normalizesHyphenAndSpaces() {
    assertThat(ReservationCode.normalize("4821-7390")).contains("48217390");
    assertThat(ReservationCode.normalize(" 4821 7390 ")).contains("48217390");
  }

  @Test
  void rejectsMalformedInput() {
    assertThat(ReservationCode.normalize(null)).isEmpty();
    assertThat(ReservationCode.normalize("128")).isEmpty();
    assertThat(ReservationCode.normalize("482173901")).isEmpty();
    assertThat(ReservationCode.normalize("4821-739O")).isEmpty();
  }
}
