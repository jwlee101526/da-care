package com.dacare.server.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 매뉴얼 문서가 모두 머리말 형식을 지키고, 진단 도구가 사용하는 기기 분류로 작성됐는지 확인한다.
 */
class ManualIngestionServiceTests {

  private final ManualIngestionService service = new ManualIngestionService(null,
      "text-embedding-3-small");

  @Test
  void everyManualHasKnownDeviceTypeAndBody() {
    List<ManualIngestionService.ManualEntry> entries = service.loadEntries();

    assertFalse(entries.isEmpty());
    entries.forEach(entry -> {
      assertDoesNotThrow(() -> DiagnosisTools.DeviceType.valueOf(entry.device()), entry.slug());
      assertFalse(entry.body().isBlank(), entry.slug());
    });
    assertEquals(entries.size(),
        entries.stream().map(ManualIngestionService.ManualEntry::slug).distinct().count());
  }
}
