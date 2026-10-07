package com.dacare.server.service.diagnosis;

import com.dacare.server.service.diagnosis.DiagnosisTools.DeviceType;
import com.dacare.server.service.diagnosis.DiagnosisTools.Page;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상담 도구가 만들어 화면에 표시하는 카드. type 값으로 웹 클라이언트가 카드 종류를 구분한다.
 */
public sealed interface DiagnosisCard permits DiagnosisCard.InspectionCard,
    DiagnosisCard.BookingCard, DiagnosisCard.ReservationStatusCard, DiagnosisCard.NavigationCard {

  record Evidence(String sourceId, String quote, String citation) {

  }

  record InspectionCard(String type, String title, DeviceType deviceType, String deviceName,
                        String suspectedCause,
                        String inspectionDetails, List<Evidence> evidence) implements
      DiagnosisCard {

  }

  /**
   * @param deviceType 예약 화면에서 고를 기기 분류
   */
  record BookingCard(String type, com.dacare.server.domain.DeviceType deviceType, String symptom,
                     boolean loginRequired) implements DiagnosisCard {

  }

  record ReservationStatusCard(String type, String reservationCode, String status,
                               LocalDateTime preferredAt,
                               LocalDateTime confirmedAt, String engineerName) implements
      DiagnosisCard {

  }

  record NavigationCard(String type, Page page, String title, String description,
                        String actionLabel) implements DiagnosisCard {

  }
}
