package com.dacare.server.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dacare.server.service.diagnosis.DiagnosisTools;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 매뉴얼 PDF가 진단 도구의 기기 분류마다 있고, 목차 항목별로 본문이 추출되는지 확인한다.
 */
class ManualIngestionServiceTests {

  private final ManualIngestionService service = new ManualIngestionService(null,
      "text-embedding-3-small");
  private final List<ManualIngestionService.ManualSection> sections = service.loadSections();

  @Test
  void everyDeviceTypeHasManualSections() {
    Set<String> devices = sections.stream().map(ManualIngestionService.ManualSection::device)
        .collect(Collectors.toSet());

    assertEquals(Arrays.stream(DiagnosisTools.DeviceType.values()).map(Enum::name)
        .collect(Collectors.toSet()), devices);
    sections.forEach(section -> {
      String name = section.fileName() + " " + section.title();
      assertDoesNotThrow(() -> DiagnosisTools.DeviceType.valueOf(section.device()), name);
      assertFalse(section.text().isBlank(), name);
    });
  }

  @Test
  void everySectionGetsDistinctVectorId() {
    // pgvector는 같은 ID를 덮어쓰므로, 같은 페이지의 섹션도 서로 다른 ID를 가져야 한다.
    assertEquals(sections.size(), sections.stream()
        .map(section -> ManualIngestionService.documentId("v1", section)).distinct().count());
  }

  @Test
  void splitsTroubleshootingTableByProblemAndDropsPageChrome() {
    var section = sections.stream()
        .filter(candidate -> candidate.title().equals("노트북 배터리가 충전되지 않음"))
        .findFirst().orElseThrow();
    String text = section.text().replaceAll("\\s+", " ");

    assertEquals("computer", section.device());
    assertEquals("PC·노트북 사용 설명서", section.manualTitle());
    // 표의 읽기 순서(문제 → 원인 → 해결 방법)가 유지된다.
    assertTrue(text.startsWith("노트북 배터리가 충전되지 않음"), text);
    assertTrue(text.indexOf("배터리 보호 모드") < text.indexOf("충전 한도를 확인하십시오"), text);
    // 같은 페이지에서 이어지는 앞뒤 문제의 내용이 섞이지 않는다.
    assertFalse(text.contains("PC 화면만"), text);
    assertFalse(text.contains("써멀 구리스"), text);
    assertFalse(text.contains("사용 설명서 /"), text);
  }
}
