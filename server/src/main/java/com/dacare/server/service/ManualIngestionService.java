package com.dacare.server.service;

import com.dacare.server.domain.ManualImport;
import com.dacare.server.repository.ManualImportRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 증상별 매뉴얼 문서(manual/*.md)를 문서 하나당 벡터 하나로 저장한다.
 * <p>
 * 문서 내용과 임베딩 모델로 버전을 계산해, 둘 중 하나라도 바뀌면 이전 버전 벡터를 지우고 다시 수집한다.
 */
@Service
public class ManualIngestionService {

  private static final String LOCATION = "classpath:manual/*.md";
  private static final String LEGACY_PDF = "device-troubleshooting.pdf";
  private final ManualImportRepository imports;
  private final String embeddingModel;

  public ManualIngestionService(ManualImportRepository imports,
      @Value("${spring.ai.openai.embedding.model:text-embedding-ada-002}") String embeddingModel) {
    this.imports = imports;
    this.embeddingModel = embeddingModel;
  }

  @Transactional
  public void ingest(VectorStore vectorStore) {
    List<ManualEntry> entries = loadEntries();
    String version = version(entries);
    String source = "manual:" + version;
    if (imports.existsBySource(source)) {
      return;
    }
    vectorStore.write(entries.stream().map(entry -> new Document(
        UUID.nameUUIDFromBytes((version + ":" + entry.slug()).getBytes(StandardCharsets.UTF_8))
            .toString(),
        entry.title() + "\n" + entry.body(),
        Map.of("source", "manual", "version", version, "slug", entry.slug(),
            "device", entry.device(), "title", entry.title()))).toList());
    FilterExpressionBuilder filter = new FilterExpressionBuilder();
    vectorStore.delete(filter.and(filter.eq("source", "manual"), filter.ne("version", version))
        .build());
    vectorStore.delete(filter.eq("file_name", LEGACY_PDF).build());
    imports.save(new ManualImport(source));
  }

  List<ManualEntry> loadEntries() {
    try {
      Resource[] resources = new PathMatchingResourcePatternResolver().getResources(LOCATION);
      List<ManualEntry> entries = Arrays.stream(resources)
          .sorted(Comparator.comparing(resource -> Objects.requireNonNull(resource.getFilename())))
          .map(ManualIngestionService::parse).toList();
      if (entries.isEmpty()) {
        throw new IllegalStateException("매뉴얼 문서가 없습니다: " + LOCATION);
      }
      return entries;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static ManualEntry parse(Resource resource) {
    String filename = Objects.requireNonNull(resource.getFilename());
    String text;
    try {
      text = resource.getContentAsString(StandardCharsets.UTF_8).replace("\r\n", "\n").strip();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    String[] parts = text.split("(?m)^---$", 3);
    if (parts.length != 3 || !parts[0].isBlank()) {
      throw new IllegalStateException("매뉴얼 머리말 형식이 올바르지 않습니다: " + filename);
    }
    Map<String, String> header = new LinkedHashMap<>();
    parts[1].strip().lines().forEach(line -> {
      int colon = line.indexOf(':');
      if (colon > 0) {
        header.put(line.substring(0, colon).strip(), line.substring(colon + 1).strip());
      }
    });
    String device = header.get("device");
    String title = header.get("title");
    String body = parts[2].strip();
    if (device == null || title == null || body.isBlank()) {
      throw new IllegalStateException("매뉴얼에 device, title, 본문이 필요합니다: " + filename);
    }
    return new ManualEntry(filename.substring(0, filename.length() - ".md".length()), device,
        title, body);
  }

  private String version(List<ManualEntry> entries) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(embeddingModel.getBytes(StandardCharsets.UTF_8));
      entries.forEach(entry -> digest.update(entry.toString().getBytes(StandardCharsets.UTF_8)));
      return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  record ManualEntry(String slug, String device, String title, String body) {

  }
}
