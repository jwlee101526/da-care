package com.dacare.server.service.diagnosis;

import com.dacare.server.service.ReservationService;
import com.dacare.server.service.diagnosis.DiagnosisCard.BookingCard;
import com.dacare.server.service.diagnosis.DiagnosisCard.Evidence;
import com.dacare.server.service.diagnosis.DiagnosisCard.InspectionCard;
import com.dacare.server.service.diagnosis.DiagnosisCard.NavigationCard;
import com.dacare.server.service.diagnosis.DiagnosisCard.ReservationStatusCard;
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

  private static final String DEVICE_TYPE_DESCRIPTION = "기기 분류: smartphone(스마트폰·태블릿), "
      + "computer(노트북·데스크탑 PC), tv(스마트 TV), console(게임 콘솔), aircon(에어컨), "
      + "washing(세탁기·건조기), fridge(냉장고·김치냉장고), microwave(전자레인지·인덕션), "
      + "cleaner(청소기·로봇청소기), internet(공유기·인터넷), audio(사운드바·스피커·이어폰), "
      + "etc(그 밖의 생활 가전)";

  private final VectorStore vectorStore;
  private final ReservationService reservations;
  private final String email;
  private final Consumer<ToolProgress> progress;
  private final double similarityThreshold;
  private final Map<String, ManualSource> sources = new LinkedHashMap<>();
  private final Map<String, DiagnosisCard> cards = new LinkedHashMap<>();
  private final List<String> executedTools = new ArrayList<>();

  public DiagnosisTools(VectorStore vectorStore, ReservationService reservations, String email,
      double similarityThreshold, Consumer<ToolProgress> progress) {
    this.vectorStore = vectorStore;
    this.reservations = reservations;
    this.email = email;
    this.similarityThreshold = similarityThreshold;
    this.progress = progress;
  }

  @Tool(description = "기기 증상에 해당하는 실제 매뉴얼을 검색합니다. 빈 결과이면 근거가 없습니다. 한 번에 기기 하나의 증상 하나만 검색하고, 여러 기기나 증상은 각각 따로 호출하세요.")
  public synchronized List<ManualSource> searchManuals(
      @ToolParam(description = "검색할 기기와 증상 하나(예: 스마트폰 화면 터치가 안 됨). 다른 기기나 이전 대화의 증상을 섞지 마세요.") String query) {
    started("searchManuals");
    requireText(query, 2000);
    if (vectorStore == null) {
      throw new IllegalStateException("매뉴얼 검색 서비스를 사용할 수 없습니다.");
    }
    var documents = vectorStore.similaritySearch(
        SearchRequest.builder().query(query).topK(3).similarityThreshold(similarityThreshold)
            .build());
    List<ManualSource> result = documents == null ? List.of() : documents.stream()
        .filter(document -> document.getText() != null && !document.getText().isBlank())
        .map(document -> new ManualSource(document.getId(), document.getText(),
            citation(document.getMetadata()))).toList();
    result.forEach(source -> sources.put(source.id(), source));
    executedTools.add("searchManuals");
    completed("searchManuals");
    return result;
  }

  @Tool(description = "검색된 매뉴얼에 근거한 점검 안내를 표시합니다. 기기마다 한 번씩 호출할 수 있습니다. sourceIds에는 searchManuals가 반환한 문서 ID만 전달하세요. 서버가 해당 원문을 근거로 첨부합니다. 근거가 없으면 호출하지 마세요.")
  public synchronized InspectionCard showInspectionCard(
      @ToolParam(description = "사용자에게 표시할 점검 제목") String title,
      @ToolParam(description = DEVICE_TYPE_DESCRIPTION) DeviceType deviceType,
      @ToolParam(description = "사용자가 설명한 기기 이름. 확인되지 않은 모델명은 쓰지 마세요.") String deviceName,
      @ToolParam(required = false, description = "매뉴얼에 명시된 가능한 원인. 원인을 알 수 없으면 null. 문서 ID를 쓰지 마세요.") String suspectedCause,
      @ToolParam(description = "매뉴얼에 근거한 점검 절차. 문서 ID 대신 사용자가 이해할 설명을 작성하세요.") String inspectionDetails,
      @ToolParam(description = "searchManuals 결과의 근거 문서 ID 목록") List<String> sourceIds) {
    started("showInspectionCard");
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
      return new Evidence(source.id(), source.text(), source.citation());
    }).toList();
    String cause =
        suspectedCause == null || suspectedCause.isBlank() || sources.containsKey(suspectedCause)
            ? null : suspectedCause;
    InspectionCard card = new InspectionCard("inspection", title, deviceType, deviceName, cause,
        inspectionDetails, List.copyOf(evidence));
    // 여러 기기를 함께 상담하면 기기별 안내가 모두 남도록 기기 분류로 구분한다.
    cards.put("inspection:" + deviceType, card);
    executedTools.add("showInspectionCard");
    completed("showInspectionCard", card);
    return card;
  }

  @Tool(description = "방문 점검 예약 입력을 준비합니다. 예약을 저장하거나 일정과 기사 배정을 확정하지 않습니다. 기기 분류가 불분명하면 etc를 사용하세요.")
  public synchronized BookingCard prepareReservation(
      @ToolParam(description = DEVICE_TYPE_DESCRIPTION) DeviceType deviceType,
      @ToolParam(description = "예약 입력에 미리 채울 기기 증상. 사용자가 설명한 증상만 한두 문장으로 정리하고, 예약 방법 문의나 예약 준비 요청 문구는 제외하세요.") String symptom) {
    started("prepareReservation");
    Objects.requireNonNull(deviceType);
    requireText(symptom, 2000);
    BookingCard card = new BookingCard("booking", deviceType, symptom.strip(), email == null);
    cards.put("booking", card);
    executedTools.add("prepareReservation");
    completed("prepareReservation", card);
    return card;
  }

  @Tool(description = "로그인한 사용자의 예약 번호로 실제 예약 상태와 배정된 엔지니어를 조회합니다. 다른 사용자의 예약에는 접근할 수 없습니다.")
  public synchronized ReservationStatusCard getReservationStatus(
      @ToolParam(description = "숫자 8자리 예약 번호(예: 4821-7390)") String reservationCode) {
    started("getReservationStatus");
    if (email == null) {
      throw new IllegalStateException("예약 조회는 로그인이 필요합니다.");
    }
    var reservation = reservations.mineOneByCode(email, reservationCode);
    ReservationStatusCard card = new ReservationStatusCard("reservation_status",
        reservation.getCode(),
        reservation.getStatus().name(), reservation.getPreferredAt(), reservation.getConfirmedAt(),
        reservation.getEngineer() == null ? null : reservation.getEngineer().getName());
    cards.put("reservation_status:" + reservation.getCode(), card);
    executedTools.add("getReservationStatus");
    completed("getReservationStatus", card);
    return card;
  }

  @Tool(description = "사용자가 요청한 실제 서비스 페이지로 안내합니다. 방문 점검 예약 신청 방법이나 신청 페이지는 reserve, 예약 내역과 점검 신청 현황은 reservations를 사용하세요.")
  public synchronized NavigationCard navigateTo(Page page) {
    started("navigateTo");
    Objects.requireNonNull(page);
    NavigationCard card = switch (page) {
      case reservations ->
          new NavigationCard("navigation", page, "예약 내역 조회", "접수한 방문 점검의 상태와 확정 일정을 확인할 수 있습니다.",
              "예약 내역으로 이동");
      case reserve -> new NavigationCard("navigation", page, "방문 점검 신청",
          "기기와 증상, 방문 희망 일시와 장소를 입력해 방문 점검을 신청할 수 있습니다.",
          "예약 신청하기");
    };
    cards.put("navigation:" + page, card);
    executedTools.add("navigateTo");
    completed("navigateTo", card);
    return card;
  }

  private void started(String tool) {
    if (Thread.currentThread().isInterrupted()) {
      throw new DiagnosisCancelledException();
    }
    progress.accept(new ToolProgress(tool, "started"));
  }

  private void completed(String tool) {
    progress.accept(new ToolProgress(tool, "completed"));
  }

  private void completed(String tool, DiagnosisCard card) {
    progress.accept(new ToolProgress(tool, "completed", card));
  }

  private void requireText(String text, int maxLength) {
    if (text == null || text.isBlank() || text.length() > maxLength) {
      throw new IllegalArgumentException("도구 입력값을 확인하세요.");
    }
  }

  public synchronized List<DiagnosisCard> cards() {
    return List.copyOf(cards.values());
  }

  public synchronized List<String> executedTools() {
    return List.copyOf(executedTools);
  }

  /**
   * 예약 기기 분류({@link com.dacare.server.domain.DeviceType}) 중 매뉴얼로 상담하는 항목. 이름이 같아 상담 카드의 값을 그대로
   * 예약에 쓸 수 있다. 긴급 출장(repair)은 증상 분류가 아니므로 뺐다. 매뉴얼 PDF도 같은 이름의 폴더로 분류한다.
   */
  public enum DeviceType {
    smartphone, computer, tv, console, aircon, washing, fridge, microwave, cleaner, internet,
    audio, etc
  }

  public enum Page {reservations, reserve}

  /**
   * 매뉴얼 제목과 시작 쪽으로 사용자에게 보여 줄 출처를 만든다. 예: PC·노트북 사용 설명서 6쪽
   */
  private static String citation(Map<String, Object> metadata) {
    Object manual = metadata.get("manual_title");
    Object page = metadata.get("start_page");
    if (manual == null) {
      return null;
    }
    return page == null ? manual.toString() : manual + " " + page + "쪽";
  }

  public record ManualSource(String id, String text, String citation) {

  }

  public record ToolProgress(String tool, String status, DiagnosisCard card) {

    public ToolProgress(String tool, String status) {
      this(tool, status, null);
    }
  }
}
