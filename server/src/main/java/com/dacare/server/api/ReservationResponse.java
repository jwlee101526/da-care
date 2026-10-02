package com.dacare.server.api;

import com.dacare.server.domain.Reservation;
import java.time.LocalDateTime;

/**
 * @param id   회원·관리자 API에서만 쓰는 내부 번호. 순번이라 전체 예약 건수가 드러나므로 비회원 응답에서는 비운다.
 * @param code 고객에게 보여주는 예약 번호(숫자 8자리)
 */
public record ReservationResponse(Long id, String code, String deviceType,
                                  String symptomDescription, String visitAddress,
                                  LocalDateTime preferredAt, LocalDateTime confirmedAt,
                                  String status, String engineerName, String contactName,
                                  String contactPhone) {

  static ReservationResponse from(Reservation reservation) {
    return of(reservation, reservation.getId(), reservation.getVisitAddress(),
        reservation.getContactName(),
        reservation.getContactPhone() == null ? null : reservation.getContactPhone().value());
  }

  /** 비회원 예약 접수 직후 응답. 방금 입력한 본인에게 돌려주는 것이므로 개인정보를 가리지 않는다. */
  static ReservationResponse forGuest(Reservation reservation) {
    return of(reservation, null, reservation.getVisitAddress(), reservation.getContactName(),
        reservation.getContactPhone().value());
  }

  /** 비회원 예약 조회·취소 응답. 이름·연락처·상세 주소를 가리며, 연락처는 표시용 문자열이다. */
  static ReservationResponse maskedForGuest(Reservation reservation) {
    return of(reservation, null, PersonalDataMask.address(reservation.getVisitAddress()),
        PersonalDataMask.name(reservation.getContactName()),
        PersonalDataMask.phone(reservation.getContactPhone()));
  }

  private static ReservationResponse of(Reservation reservation, Long id, String visitAddress,
      String contactName, String contactPhone) {
    return new ReservationResponse(id, reservation.getCode(), reservation.getDeviceType(),
        reservation.getSymptomDescription(), visitAddress, reservation.getPreferredAt(),
        reservation.getConfirmedAt(), reservation.getStatus().name(),
        reservation.getEngineer() == null ? null : reservation.getEngineer().getName(),
        contactName, contactPhone);
  }
}
