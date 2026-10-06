package com.dacare.server.service.usage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 유료 API의 주간 호출 한도. 매주 월요일 0시(한국 시간)에 초기화된다.
 *
 * @param diagnosisTotal    서비스 전체 AI 상담 한도
 * @param diagnosisCustomer 일반 계정 1개의 AI 상담 한도
 * @param diagnosisAdmin    관리자 계정 1개의 AI 상담 한도
 * @param diagnosisGuest    비로그인 사용자(IP 1개)의 AI 상담 한도
 * @param sms               서비스 전체 SMS 발송 한도
 */
@ConfigurationProperties("app.usage.weekly-limit")
public record UsageLimits(int diagnosisTotal, int diagnosisCustomer, int diagnosisAdmin,
                          int diagnosisGuest, int sms) {

  int diagnosis(UsageSubject subject) {
    return switch (subject.type()) {
      case CUSTOMER -> diagnosisCustomer;
      case ADMIN -> diagnosisAdmin;
      case GUEST -> diagnosisGuest;
    };
  }
}
