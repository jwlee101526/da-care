package com.dacare.server.service;

import com.dacare.server.domain.DeviceType;
import com.dacare.server.domain.PhoneNumber;
import java.time.LocalDateTime;

/**
 * 예약 접수 입력값. 회원 예약은 연락처가 비어 있으면 고객 정보로 채운다.
 */
public record ReservationDraft(DeviceType deviceType, String symptomDescription, String visitAddress,
                               LocalDateTime preferredAt, String contactName,
                               PhoneNumber contactPhone) {

}
