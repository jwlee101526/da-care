package com.dacare.server.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

public class DiagnosisTools {

  private final VectorStore vectorStore;
  private final ReservationService reservations;
  private final String question;
  private final String email;
  private final Consumer<ToolProgress> progress;
  private final Map<String, ManualSource> sources = new LinkedHashMap<>();
  private final Map<String, Card> cards = new LinkedHashMap<>();
  private final List<String> executedTools = new ArrayList<>();
  private int calls;

  public DiagnosisTools(VectorStore vectorStore, ReservationService reservations, String question,
      String email) {
    this(vectorStore, reservations, question, email, ignored -> {
    });
  }

  public DiagnosisTools(VectorStore vectorStore, ReservationService reservations, String question,
      String email, Consumer<ToolProgress> progress) {
    this.vectorStore = vectorStore;
    this.reservations = reservations;
    this.question = question;
    this.email = email;
    this.progress = progress;
  }

  @Tool(description = "기기 증상에 해당하는 실제 매뉴얼을 검색합니다. 빈 결과이면 근거가 없습니다.")
  public synchronized List<ManualSource> searchManuals(String query) {
    started("searchManuals");
    countCall();
    requireText(query, 2000);
    if (vectorStore == null) {
      throw new IllegalStateException("매뉴얼 검색 서비스를 사용할 수 없습니다.");
    }
    var documents = vectorStore.similaritySearch(
        SearchRequest.builder().query(query).topK(3).similarityThreshold(0.65).build());
    List<ManualSource> result = documents == null ? List.of() : documents.stream()
        .filter(document -> document.getText() != null && !document.getText().isBlank())
        .map(document -> new ManualSource(document.getId(), document.getText())).toList();
    result.forEach(source -> sources.put(source.id(), source));
    executedTools.add("searchManuals");
    completed("searchManuals");
    return result;
  }

  @Tool(description = "검색된 매뉴얼에 근거한 점검 안내를 표시합니다. sourceIds에는 searchManuals가 반환한 문서 ID만 전달하세요. 서버가 해당 원문을 근거로 첨부합니다. 근거가 없으면 호출하지 마세요.")
  public synchronized InspectionCard showInspectionCard(
      @ToolParam(description = "사용자에게 표시할 점검 제목") String title,
      DeviceType deviceType,
      @ToolParam(description = "사용자가 설명한 기기 이름. 확인되지 않은 모델명은 쓰지 마세요.") String deviceName,
      @ToolParam(required = false, description = "매뉴얼에 명시된 가능한 원인. 원인을 알 수 없으면 null. 문서 ID를 쓰지 마세요.") String suspectedCause,
      @ToolParam(description = "매뉴얼에 근거한 점검 절차. 문서 ID 대신 사용자가 이해할 설명을 작성하세요.") String inspectionDetails,
      @ToolParam(description = "searchManuals 결과의 근거 문서 ID 목록") List<String> sourceIds) {
    started("showInspectionCard");
    countCall();
    requireText(title, 100);
    Objects.requireNonNull(deviceType);
    requireText(deviceName, 100);
    if (suspectedCause != null && !suspectedCause.isBlank()) {
      requireText(suspectedCause, 500);
    }
    requireText(inspectionDetails, 1000);
    if (sourceIds == null || sourceIds.isEmpty() || sourceIds.size() > 3) {
      throw new IllegalArgumentException("매뉴얼 근거가 필요합니다.");
    }
    List<Evidence> evidence = sourceIds.stream().distinct().map(id -> {
      ManualSource source = sources.get(id);
      if (source == null) {
        throw new IllegalArgumentException("검색된 매뉴얼과 일치하지 않는 문서 ID입니다.");
      }
      return new Evidence(source.id(), source.text());
    }).toList();
    String cause =
        suspectedCause == null || suspectedCause.isBlank() || sources.containsKey(suspectedCause)
            ? null : suspectedCause;
    InspectionCard card = new InspectionCard("inspection", title, deviceType, deviceName, cause,
        inspectionDetails, List.copyOf(evidence));
    cards.put("inspection", card);
    executedTools.add("showInspectionCard");
    completed("showInspectionCard", card);
    return card;
  }

  @Tool(description = "방문 점검 예약 입력을 준비합니다. 예약을 저장하거나 일정과 기사 배정을 확정하지 않습니다. 기기 분류가 불분명하면 etc를 사용하세요.")
  public synchronized BookingCard prepareReservation(DeviceType deviceType) {
    started("prepareReservation");
    countCall();
    Objects.requireNonNull(deviceType);
    BookingCard card = new BookingCard("booking", deviceType, question, email == null);
    cards.put("booking", card);
    executedTools.add("prepareReservation");
    completed("prepareReservation", card);
    return card;
  }

  @Tool(description = "로그인한 사용자의 예약 번호로 실제 예약 상태와 배정된 엔지니어를 조회합니다. 다른 사용자의 예약에는 접근할 수 없습니다.")
  public synchronized ReservationStatusCard getReservationStatus(long reservationId) {
    started("getReservationStatus");
    countCall();
    if (email == null) {
      throw new IllegalStateException("예약 조회는 로그인이 필요합니다.");
    }
    var reservation = reservations.mineOne(email, reservationId);
    ReservationStatusCard card = new ReservationStatusCard("reservation_status",
        reservation.getId(),
        reservation.getStatus().name(), reservation.getPreferredAt(), reservation.getConfirmedAt(),
        reservation.getEngineer() == null ? null : reservation.getEngineer().getName());
    cards.put("reservation_status:" + reservationId, card);
    executedTools.add("getReservationStatus");
    completed("getReservationStatus", card);
    return card;
  }

  @Tool(description = "사용자가 요청한 실제 서비스 페이지로 안내합니다. 예약 페이지, 예약 내역, 점검 신청 현황은 reservations를 사용하세요.")
  public synchronized NavigationCard navigateTo(Page page) {
    started("navigateTo");
    countCall();
    Objects.requireNonNull(page);
    NavigationCard card = switch (page) {
      case reservations ->
          new NavigationCard("navigation", page, "예약 내역 조회", "접수한 방문 점검의 상태와 확정 일정을 확인할 수 있습니다.",
              "예약 내역으로 이동");
    };
    cards.put("navigation:" + page, card);
    executedTools.add("navigateTo");
    completed("navigateTo", card);
    return card;
  }

  private void countCall() {
    if (++calls > 8) {
      throw new IllegalStateException("도구 호출 횟수를 초과했습니다.");
    }
  }

  private void started(String tool) {
    progress.accept(new ToolProgress(tool, "started"));
  }

  private void completed(String tool) {
    progress.accept(new ToolProgress(tool, "completed"));
  }

  private void completed(String tool, Card card) {
    progress.accept(new ToolProgress(tool, "completed", card));
  }

  private void requireText(String text, int maxLength) {
    if (text == null || text.isBlank() || text.length() > maxLength) {
      throw new IllegalArgumentException("도구 입력값을 확인하세요.");
    }
  }

  public synchronized List<Card> cards() {
    return List.copyOf(cards.values());
  }

  public synchronized List<String> executedTools() {
    return List.copyOf(executedTools);
  }

  public enum DeviceType {laptop, smartphone, appliance, etc}

  public enum Page {reservations}

  public sealed interface Card permits InspectionCard, BookingCard, ReservationStatusCard,
      NavigationCard {

  }

  public record ManualSource(String id, String text) {

  }

  public record Evidence(String sourceId, String quote) {

  }

  public record InspectionCard(String type, String title, DeviceType deviceType, String deviceName,
                               String suspectedCause,
                               String inspectionDetails, List<Evidence> evidence) implements Card {

  }

  public record BookingCard(String type, DeviceType deviceType, String symptom,
                            boolean loginRequired) implements Card {

  }

  public record ReservationStatusCard(String type, Long reservationId, String status,
                                      LocalDateTime preferredAt,
                                      LocalDateTime confirmedAt, String engineerName) implements
      Card {

  }

  public record NavigationCard(String type, Page page, String title, String description,
                               String actionLabel) implements Card {

  }

  public record ToolProgress(String tool, String status, Card card) {

    public ToolProgress(String tool, String status) {
      this(tool, status, null);
    }
  }
}
