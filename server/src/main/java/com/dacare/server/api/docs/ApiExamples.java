package com.dacare.server.api.docs;

/**
 * API 문서의 요청/응답 Example. 시그니처가 길어지지 않도록 Docs 인터페이스에서 분리함.
 */
final class ApiExamples {

  static final String SIGNUP = """
      {"email":"user@example.com","password":"password1234","name":"홍길동",
       "phone":"010-1234-5678","address":"서울특별시 강남구 테헤란로 1"}
      """;

  static final String LOGIN = """
      {"email":"user@example.com","password":"password1234"}
      """;

  static final String QUESTION = """
      {"question":"세탁기에서 탈수할 때 큰 소리가 납니다.","history":[]}
      """;

  static final String RESERVATION = """
      {"deviceType":"washing","symptomDescription":"탈수 시 큰 소음 발생",
       "visitAddress":"서울특별시 강남구 테헤란로 1","preferredAt":"2026-10-01T14:00:00",
       "contactName":"홍길동","contactPhone":"010-1234-5678"}
      """;

  static final String GUEST_LOOKUP = """
      {"reservationCode":"4821-7390","contactPhone":"010-1234-5678"}
      """;

  static final String GUEST_CANCEL = """
      {"contactPhone":"010-1234-5678"}
      """;

  static final String ENGINEER = """
      {"name":"김기사","phone":"010-9876-5432","specialty":"생활가전","region":"서울"}
      """;

  static final String CONFIRMATION = """
      {"engineerId":1,"confirmedAt":"2026-10-01T14:00:00"}
      """;

  static final String DIAGNOSIS_STREAM = """
      event: tool
      data: {"message":"매뉴얼을 검색하고 있습니다."}

      event: completed
      data: {"answer":"..."}
      """;

  private ApiExamples() {
  }
}
