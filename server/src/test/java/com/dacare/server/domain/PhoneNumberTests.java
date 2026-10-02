package com.dacare.server.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTests {

  @ParameterizedTest
  @CsvSource({
      "010-1234-5678, +821012345678, 010-1234-5678",
      "01012345678, +821012345678, 010-1234-5678",
      "+82 10-1234-5678, +821012345678, 010-1234-5678",
      "(02) 123-4567, +8221234567, 02-123-4567",
      "02-1234-5678, +82212345678, 02-1234-5678",
      "031.123.4567, +82311234567, 031-123-4567",
      "+1 201-555-0123, +12015550123, +1 201-555-0123"
  })
  void normalizesInputToE164AndFormatsForDisplay(String input, String value, String formatted) {
    PhoneNumber phone = PhoneNumber.of(input);

    assertThat(phone.value()).isEqualTo(value);
    assertThat(phone.format()).isEqualTo(formatted);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "0101234", "010123456789", "010-abcd-5678", "+82 1234"})
  void rejectsInvalidInput(String input) {
    assertThat(PhoneNumber.isValid(input)).isFalse();
    assertThatThrownBy(() -> PhoneNumber.of(input)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void comparesByNormalizedValue() {
    assertThat(PhoneNumber.of("010-1234-5678")).isEqualTo(PhoneNumber.of("+82 10 1234 5678"));
  }

  @Test
  void treatsBlankOptionalInputAsAbsent() {
    assertThat(PhoneNumber.ofNullableMobile(null)).isNull();
    assertThat(PhoneNumber.ofNullableMobile(" ")).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"010-1234-5678", "+1 201-555-0123", "+44 7400 123456"})
  void acceptsMobileNumbers(String input) {
    assertThat(PhoneNumber.isValidMobile(input)).isTrue();
    assertThat(PhoneNumber.ofMobile(input).isMobile()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"02-1234-5678", "031-123-4567", "+44 20 7031 3000"})
  void rejectsLandlineWhereMobileIsRequired(String input) {
    assertThat(PhoneNumber.isValid(input)).isTrue();
    assertThat(PhoneNumber.isValidMobile(input)).isFalse();
    assertThatThrownBy(() -> PhoneNumber.ofMobile(input))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void providesNumbersForDomesticAndInternationalDialing() {
    PhoneNumber domestic = PhoneNumber.of("010-1234-5678");
    PhoneNumber foreign = PhoneNumber.of("+44 20 7031 3000");

    assertThat(domestic.isDomestic()).isTrue();
    assertThat(domestic.nationalDialingNumber()).isEqualTo("01012345678");
    assertThat(foreign.isDomestic()).isFalse();
    assertThat(foreign.countryCode()).isEqualTo(44);
    assertThat(foreign.nationalSignificantNumber()).isEqualTo("2070313000");
  }

  @Test
  void convertsToAndFromE164Column() {
    PhoneNumberConverter converter = new PhoneNumberConverter();

    assertThat(converter.convertToDatabaseColumn(PhoneNumber.of("010-1234-5678")))
        .isEqualTo("+821012345678");
    assertThat(converter.convertToEntityAttribute("+821012345678"))
        .isEqualTo(PhoneNumber.of("010-1234-5678"));
  }
}
