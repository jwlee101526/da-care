package com.dacare.server.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  /**
   * 일일 사용량의 날짜 경계는 서버 시간대와 관계없이 한국 시간 자정으로 둔다.
   */
  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
  }
}
