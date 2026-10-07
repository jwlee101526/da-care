package com.dacare.server.api.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.dacare.server.domain.PhoneNumber;
import org.junit.jupiter.api.Test;

class PersonalDataMaskTests {

  @Test
  void masksName() {
    assertThat(PersonalDataMask.name("홍길동")).isEqualTo("홍*동");
    assertThat(PersonalDataMask.name("남궁민수")).isEqualTo("남**수");
    assertThat(PersonalDataMask.name("김철")).isEqualTo("김*");
    assertThat(PersonalDataMask.name("이")).isEqualTo("*");
  }

  @Test
  void keepsFirstGroupAndLastFourDigitsOfPhone() {
    assertThat(PersonalDataMask.phone(PhoneNumber.of("010-1234-5678"))).isEqualTo("010-****-5678");
    assertThat(PersonalDataMask.phone(PhoneNumber.of("+1 201-555-0123")))
        .isEqualTo("+1 ***-***-0123");
    assertThat(PersonalDataMask.phone(null)).isNull();
  }

  @Test
  void keepsOnlyRegionOfAddress() {
    assertThat(PersonalDataMask.address("서울특별시 강남구 테헤란로 1 101동 1203호"))
        .isEqualTo("서울특별시 강남구 ***");
    assertThat(PersonalDataMask.address("세종시 한누리대로 2130")).isEqualTo("세종시 한누리대로 ***");
    assertThat(PersonalDataMask.address("판교역로 1")).isEqualTo("판교역로 ***");
    assertThat(PersonalDataMask.address("어딘가")).isEqualTo("***");
  }
}
