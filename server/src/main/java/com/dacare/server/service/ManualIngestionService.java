package com.dacare.server.service;

import com.dacare.server.domain.ManualImport;
import com.dacare.server.repository.ManualImportRepository;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.ai.document.ContentFormatter;
import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제품 매뉴얼 PDF(manual/{기기 분류}/*.pdf)를 목차 항목 하나당 벡터 하나로 저장한다.
 * <p>
 * 목차의 최하위 항목이 가리키는 위치부터 다음 목차 항목 직전까지를 한 섹션으로 보므로, 한 페이지에 여러 섹션이 있어도
 * 나뉜다. 목차가 없는 PDF는 페이지 단위로 나눈다. 기기 분류는 상위 폴더 이름으로 정한다.
 * <p>
 * 매뉴얼 파일과 임베딩 모델로 버전을 계산해, 둘 중 하나라도 바뀌면 이전 버전 벡터를 지우고 다시 수집한다.
 */
@Service
public class ManualIngestionService {

  private static final String LOCATION = "classpath:manual/*/*.pdf";
  private static final String LEGACY_PDF = "device-troubleshooting.pdf";
  /**
   * 섹션 본문만으로는 어느 제품 매뉴얼인지 알 수 없으므로 매뉴얼 제목과 섹션 제목만 본문과 함께 임베딩한다.
   */
  private static final ContentFormatter EMBED_FORMAT = DefaultContentFormatter.builder()
      .withExcludedEmbedMetadataKeys("source", "version", "device", "file_name", "start_page",
          "end_page")
      .build();
  private final ManualImportRepository imports;
  private final String embeddingModel;

  public ManualIngestionService(ManualImportRepository imports,
      @Value("${spring.ai.openai.embedding.model:text-embedding-ada-002}") String embeddingModel) {
    this.imports = imports;
    this.embeddingModel = embeddingModel;
  }

  @Transactional
  public void ingest(VectorStore vectorStore) {
    List<ManualFile> files = loadFiles();
    String version = version(files);
    String source = "manual:" + version;
    if (imports.existsBySource(source)) {
      return;
    }
    vectorStore.write(files.stream().flatMap(file -> parse(file).stream()).map(section -> {
      Document document = new Document(documentId(version, section), section.text(),
          Map.of("source", "manual", "version", version, "device", section.device(),
              "file_name", section.fileName(), "manual_title", section.manualTitle(),
              "title", section.title(),
              "start_page", section.startPage(), "end_page", section.endPage()));
      document.setContentFormatter(EMBED_FORMAT);
      return document;
    }).toList());
    FilterExpressionBuilder filter = new FilterExpressionBuilder();
    vectorStore.delete(filter.and(filter.eq("source", "manual"), filter.ne("version", version))
        .build());
    vectorStore.delete(filter.eq("file_name", LEGACY_PDF).build());
    imports.save(new ManualImport(source));
  }

  /**
   * 벡터 ID. 한 페이지에 여러 섹션이 있으므로 쪽 번호가 아니라 섹션 제목으로 구분한다. 같은 ID는 저장 시 덮어쓰인다.
   */
  static String documentId(String version, ManualSection section) {
    return UUID.nameUUIDFromBytes((version + ":" + section.fileName() + ":" + section.title())
        .getBytes(StandardCharsets.UTF_8)).toString();
  }

  List<ManualSection> loadSections() {
    return loadFiles().stream().flatMap(file -> parse(file).stream()).toList();
  }

  private List<ManualFile> loadFiles() {
    try {
      Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
      List<ManualFile> files = Arrays.stream(resources)
          .map(ManualIngestionService::read)
          .sorted(Comparator.comparing(ManualFile::device).thenComparing(ManualFile::fileName))
          .toList();
      if (files.isEmpty()) {
        throw new IllegalStateException("매뉴얼 PDF가 없습니다: " + LOCATION);
      }
      return files;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static ManualFile read(Resource resource) {
    try {
      String[] path = resource.getURI().toString().split("/");
      return new ManualFile(path[path.length - 2], path[path.length - 1],
          resource.getContentAsByteArray());
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static List<ManualSection> parse(ManualFile file) {
    try (PDDocument document = Loader.loadPDF(file.content())) {
      int pageCount = document.getNumberOfPages();
      List<String> pages = new ArrayList<>();
      PDFTextStripper stripper = new PDFTextStripper();
      for (int page = 1; page <= pageCount; page++) {
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        pages.add(stripper.getText(document));
      }
      Set<String> boilerplate = repeatedLines(pages);
      String manualTitle = document.getDocumentInformation().getTitle();
      if (manualTitle == null || manualTitle.isBlank()) {
        manualTitle = file.fileName();
      }

      List<Heading> headings = new ArrayList<>();
      if (document.getDocumentCatalog().getDocumentOutline() != null) {
        collectHeadings(document, document.getDocumentCatalog().getDocumentOutline(), headings);
      }
      if (headings.isEmpty()) {
        for (int page = 1; page <= pageCount; page++) {
          headings.add(new Heading(file.fileName() + " " + page + "쪽", page, Float.MAX_VALUE,
              true));
        }
      }
      // 같은 페이지에서는 위쪽(top 값이 큰) 항목이 먼저 온다.
      headings.sort(Comparator.comparingInt(Heading::page)
          .thenComparing(Heading::top, Comparator.reverseOrder()));

      List<ManualSection> sections = new ArrayList<>();
      for (int i = 0; i < headings.size(); i++) {
        Heading heading = headings.get(i);
        if (!heading.leaf()) {
          continue;
        }
        Heading next = i + 1 < headings.size() ? headings.get(i + 1) : null;
        List<String> pageTexts = textBetween(document, heading, next);
        StringBuilder text = new StringBuilder();
        int endPage = heading.page();
        for (int offset = 0; offset < pageTexts.size(); offset++) {
          String body = pageTexts.get(offset).lines()
              .filter(line -> !line.isBlank() && !boilerplate.contains(normalize(line)))
              .collect(Collectors.joining("\n"));
          if (!body.isBlank()) {
            text.append(body).append('\n');
            endPage = heading.page() + offset;
          }
        }
        if (!text.isEmpty()) {
          sections.add(new ManualSection(file.device(), file.fileName(), manualTitle.strip(),
              heading.title(), heading.page(), endPage, text.toString().strip()));
        }
      }
      return sections;
    } catch (IOException exception) {
      throw new UncheckedIOException("매뉴얼 PDF를 읽을 수 없습니다: " + file.fileName(), exception);
    }
  }

  /**
   * 목차 항목과 그 항목이 가리키는 위치를 모은다. 하위 항목이 없는 항목만 섹션이 되고, 상위 항목은 앞 섹션의 끝을 정하는 데만 쓴다.
   */
  private static void collectHeadings(PDDocument document, PDOutlineNode node,
      List<Heading> headings) throws IOException {
    for (PDOutlineItem item : node.children()) {
      PDPage page = item.findDestinationPage(document);
      if (page != null && item.getTitle() != null) {
        PDDestination destination = item.getDestination();
        if (destination == null && item.getAction() instanceof PDActionGoTo goTo) {
          destination = goTo.getDestination();
        }
        if (destination instanceof PDNamedDestination named) {
          destination = document.getDocumentCatalog().findNamedDestinationPage(named);
        }
        float top = destination instanceof PDPageXYZDestination xyz && xyz.getTop() >= 0
            ? xyz.getTop() : page.getCropBox().getUpperRightY();
        headings.add(new Heading(item.getTitle().strip(), document.getPages().indexOf(page) + 1,
            top, !item.hasChildren()));
      }
      if (item.hasChildren()) {
        collectHeadings(document, item, headings);
      }
    }
  }

  /**
   * from 위치부터 다음 목차 위치 직전까지의 텍스트를 from 페이지부터 한 페이지씩 추출한다. 다음 항목이 없으면 문서 끝까지 읽는다.
   */
  private static List<String> textBetween(PDDocument document, Heading from, Heading to)
      throws IOException {
    int lastPage = to == null ? document.getNumberOfPages() : to.page();
    List<String> texts = new ArrayList<>();
    for (int pageNumber = from.page(); pageNumber <= lastPage; pageNumber++) {
      PDPage page = document.getPage(pageNumber - 1);
      PDRectangle box = page.getCropBox();
      // PDF 좌표는 아래에서 위로, 추출 영역은 위에서 아래로 잰다.
      float start = pageNumber == from.page()
          ? Math.max(0, box.getUpperRightY() - from.top()) : 0;
      float end = to != null && pageNumber == to.page()
          ? Math.max(0, box.getUpperRightY() - to.top()) : box.getHeight();
      if (end <= start) {
        texts.add("");
        continue;
      }
      // 위치 정렬 대신 PDF 내용 순서로 읽는다. 표의 칸을 가로로 섞지 않고 '문제 → 원인 → 해결 방법' 순서를 유지한다.
      PDFTextStripperByArea stripper = new PDFTextStripperByArea();
      stripper.addRegion("section", new Rectangle2D.Float(0, start, box.getWidth(), end - start));
      stripper.extractRegions(page);
      texts.add(stripper.getTextForRegion("section"));
    }
    return texts;
  }

  /**
   * 머리글·바닥글처럼 절반이 넘는 페이지에 반복되는 줄을 찾는다. 쪽 번호만 다른 줄도 같은 줄로 본다.
   */
  private static Set<String> repeatedLines(List<String> pages) {
    if (pages.size() < 3) {
      return Set.of();
    }
    return pages.stream()
        .flatMap(page -> page.lines().map(ManualIngestionService::normalize)
            .filter(line -> !line.isBlank()).distinct())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet().stream()
        .filter(entry -> entry.getValue() * 2 > pages.size())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private static String normalize(String line) {
    return line.replaceAll("\\d+", "#").strip();
  }

  private String version(List<ManualFile> files) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(embeddingModel.getBytes(StandardCharsets.UTF_8));
      for (ManualFile file : files) {
        digest.update((file.device() + "/" + file.fileName()).getBytes(StandardCharsets.UTF_8));
        digest.update(file.content());
      }
      return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private record ManualFile(String device, String fileName, byte[] content) {

  }

  private record Heading(String title, int page, float top, boolean leaf) {

  }

  record ManualSection(String device, String fileName, String manualTitle, String title,
                       int startPage, int endPage, String text) {

  }
}
