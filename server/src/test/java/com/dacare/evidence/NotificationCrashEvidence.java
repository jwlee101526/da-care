package com.dacare.evidence;

import com.dacare.server.domain.Reservation;
import com.dacare.server.notification.SlackNotificationSender;
import com.dacare.server.notification.SolapiSmsNotificationSender;
import com.dacare.server.repository.ReservationRepository;
import com.dacare.server.service.ReservationService;
import com.dacare.server.service.OutboxExperimentHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;

/**
 * 예약 커밋 직후 프로세스가 종료될 때 기존 알림 처리와 테스트 전용 Outbox 방식의 복구 결과를 비교한다.
 * 실행 결과는 포트폴리오 증빙 파일로 저장되며, 운영 애플리케이션 빈에는 포함되지 않는다.
 */
public class NotificationCrashEvidence {

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = Reservation.class)
  @EnableJpaRepositories(basePackageClasses = ReservationRepository.class)
  @Import({ReservationService.class, SlackNotificationSender.class, SolapiSmsNotificationSender.class,
      BaselineConfiguration.class, PrototypeConfiguration.class})
  public static class EvidenceConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Profile("baseline-experiment")
  @ComponentScan(basePackages = "com.dacare.server.service", useDefaultFilters = false,
      includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
          pattern = "com\\.dacare\\.server\\.service\\.NotificationEventHandler"))
  public static class BaselineConfiguration {
  }

  @Configuration(proxyBeanMethods = false)
  @Profile("outbox-experiment")
  @Import({OutboxExperimentHandler.class, SchedulingConfiguration.class})
  public static class PrototypeConfiguration {
    @Bean
    OutboxPrototype outboxPrototype(JdbcTemplate jdbc, PlatformTransactionManager manager,
        @Value("${app.slack.webhook-url}") String endpoint) {
      return new OutboxPrototype(jdbc, manager, endpoint);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  @ConditionalOnProperty(name = "app.notification.worker.enabled", havingValue = "true")
  public static class SchedulingConfiguration {
  }

  public static void main(String[] args) throws Exception {
    System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8));
    System.setErr(new java.io.PrintStream(System.err, true, StandardCharsets.UTF_8));
    if (args[0].equals("child")) {
      runChild(args[1], args[2], args[3], args[4]);
      return;
    }
    String label = args[0];
    if (!label.matches("before|after")) {
      throw new IllegalArgumentException("실험 구분은 before 또는 after여야 합니다.");
    }
    Path output = Path.of(args.length > 1 ? args[1] : "../docs/portfolio/evidence/outbox", label)
        .toAbsolutePath().normalize();
    Files.createDirectories(output);
    Path database = Files.createTempDirectory(Path.of("build"), "outbox-" + label + "-").toAbsolutePath();
    AtomicInteger control = new AtomicInteger();
    AtomicInteger deliveries = new AtomicInteger();
    HttpServer mock = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    mock.createContext("/", exchange -> {
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      boolean isControl = exchange.getRequestURI().getPath().equals("/control");
      (isControl ? control : deliveries).incrementAndGet();
      Files.writeString(output.resolve(isControl ? "control-request.json" : "recovered-request.json"), body);
      byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, response.length);
      exchange.getResponseBody().write(response);
      exchange.close();
    });
    mock.start();
    try {
      String endpoint = "http://127.0.0.1:" + mock.getAddress().getPort();
      String controlUrl = databaseUrl(database.resolve("control"));
      require(child("normal", controlUrl, endpoint + "/control", label, output.resolve("01-control.log")) == 0,
          "정상 발송 대조군 실행 실패");
      require(control.get() == 1, "정상 대조군의 테스트 서버 수신 건수 불일치");
      require(snapshot(controlUrl).get("histories") == 1, "정상 대조군의 이력 저장 실패");
      String url = databaseUrl(database.resolve("crash"));
      int exit = child("crash", url, endpoint + "/delivery", label, output.resolve("02-crash.log"));
      require(exit == 73, "지정한 커밋 후 지점에서 종료되지 않음");
      Map<String, Long> crashed = snapshot(url);
      int beforeRestart = deliveries.get();
      require(child("recover", url, endpoint + "/delivery", label, output.resolve("03-restart.log")) == 0,
          "재시작 실행 실패");
      Map<String, Long> recovered = snapshot(url);
      require(crashed.get("reservations") == 1 && beforeRestart == 0,
          "예약 커밋 후 발송 전 종료 조건 불충족");
      long expected = label.equals("after") ? 1 : 0;
      require(deliveries.get() == expected && recovered.get("histories") == expected,
          "개선 전후 기대 결과 불일치");
      String summary = """
          {
            "experiment": "%s",
            "scope": "Existing production code vs test-only Outbox prototype; not deployed",
            "recordedAt": "%s",
            "environment": "Windows / Java %s / H2 file DB (PostgreSQL mode) / local HTTP mock",
            "fault": "Runtime.halt(73) in TransactionSynchronization.afterCommit",
            "controlHttpRequests": %d,
            "crashExitCode": %d,
            "reservationsAfterCrash": %d,
            "outboxAfterCrash": %d,
            "historyAfterCrash": %d,
            "httpRequestsBeforeRestart": %d,
            "reservationsAfterRestart": %d,
            "outboxAfterRestart": %d,
            "completedOutboxAfterRestart": %d,
            "historyAfterRestart": %d,
            "httpRequestsAfterRestart": %d,
            "assertionsPassed": true
          }
          """.formatted(label, java.time.Instant.now(), System.getProperty("java.version"),
          control.get(), exit, crashed.get("reservations"), crashed.get("outbox"),
          crashed.get("histories"), beforeRestart, recovered.get("reservations"),
          recovered.get("outbox"), recovered.get("completed"), recovered.get("histories"), deliveries.get());
      Files.writeString(output.resolve("summary.json"), summary);
      Files.writeString(output.resolve("database-snapshots.txt"),
          "강제 종료 후: " + crashed + "\n재시작 후: " + recovered + "\n");
      System.out.println(summary);
    } finally {
      mock.stop(0);
    }
  }

  private static String databaseUrl(Path path) {
    return "jdbc:h2:file:" + path.toString().replace('\\', '/')
        + ";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0";
  }

  private static int child(String mode, String url, String endpoint, String variant, Path log) throws Exception {
    Path arguments = Files.createTempFile(Path.of("build"), "outbox-jvm-", ".args");
    var entries = new java.util.LinkedHashSet<String>();
    for (ClassLoader loader = Thread.currentThread().getContextClassLoader(); loader != null; loader = loader.getParent()) {
      if (loader instanceof java.net.URLClassLoader urls) {
        for (var entry : urls.getURLs()) {
          entries.add(Path.of(entry.toURI()).toString());
        }
      }
    }
    entries.addAll(java.util.Arrays.asList(System.getProperty("java.class.path").split(java.io.File.pathSeparator)));
    String classpath = String.join(java.io.File.pathSeparator, entries).replace('\\', '/');
    Files.writeString(arguments, "-Dfile.encoding=UTF-8\n-cp\n\"" + classpath + "\"\n"
        + NotificationCrashEvidence.class.getName() + "\nchild\n" + mode + "\n\"" + url
        + "\"\n" + endpoint + "\n" + variant + "\n");
    Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "@" + arguments.toAbsolutePath()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    if (!process.waitFor(60, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("실험 JVM 시간 초과: " + log);
    }
    Files.delete(arguments);
    return process.exitValue();
  }

  public static ConfigurableApplicationContext startContext(String mode, String url, String endpoint, String variant) {
    return new SpringApplicationBuilder(EvidenceConfiguration.class)
        .web(WebApplicationType.NONE).profiles(variant.equals("after") ? "outbox-experiment" : "baseline-experiment").run(
            "--spring.datasource.url=" + url, "--spring.datasource.username=sa", "--spring.datasource.password=",
            "--spring.flyway.enabled=true", "--spring.flyway.locations=classpath:db/postgresql",
            "--spring.jpa.hibernate.ddl-auto=validate", "--spring.jpa.open-in-view=false",
            "--spring.ai.model.chat=none", "--spring.ai.model.embedding=none",
            "--spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
            "--app.slack.webhook-url=" + endpoint, "--app.solapi.api-key=", "--app.solapi.api-secret=",
            "--app.solapi.sender=", "--app.notification.worker.enabled=" + !mode.equals("crash"),
            "--app.notification.poll-delay=100", "--logging.level.root=WARN");
  }

  private static void runChild(String mode, String url, String endpoint, String variant) throws Exception {
    try (ConfigurableApplicationContext context = startContext(mode, url, endpoint, variant)) {
      if (!mode.equals("recover")) {
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.executeWithoutResult(status -> {
          Reservation reservation = context.getBean(ReservationService.class).createGuest("computer",
              "장애 재현용 가상 접수", "테스트 주소", LocalDateTime.now().plusDays(2),
              "검증용 고객", "01000000000", null);
          System.out.println("[실험] 예약 저장 및 이벤트 발행: reservationId=" + reservation.getId());
          if (mode.equals("crash")) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
              @Override
              public void afterCommit() {
                System.out.println("[장애 주입] 예약 커밋 완료 / 알림 호출 전 / Runtime.halt(73)");
                System.out.flush();
                Runtime.getRuntime().halt(73);
              }
            });
          }
        });
      }
      if (variant.equals("after")) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (snapshot(url).get("histories") == 0 && System.nanoTime() < deadline) {
          Thread.sleep(100);
        }
        require(snapshot(url).get("histories") == 1, "스케줄러가 미처리 요청을 복구하지 못함");
        System.out.println("[재시작] 스케줄러의 미처리 알림 자동 발송 및 이력 저장 확인");
      } else {
        System.out.println("[재시작] 기존 구현: 영속화된 발송 요청 및 복구 작업자 없음");
      }
      System.out.println("[DB] " + snapshot(url));
    }
  }

  private static Map<String, Long> snapshot(String url) throws Exception {
    Map<String, Long> result = new LinkedHashMap<>();
    try (var connection = DriverManager.getConnection(url, "sa", "")) {
      for (String table : new String[]{"reservation", "notification_history", "notification_outbox"}) {
        long count = -1;
        try (var statement = connection.createStatement()) {
          var rows = statement.executeQuery("SELECT COUNT(*) FROM " + table);
          rows.next();
          count = rows.getLong(1);
        } catch (java.sql.SQLException e) {
          if (!table.equals("notification_outbox") || !"42S02".equals(e.getSQLState())) {
            throw e;
          }
        }
        result.put(switch (table) {
          case "reservation" -> "reservations";
          case "notification_history" -> "histories";
          default -> "outbox";
        }, count);
      }
      long completed = 0;
      if (result.get("outbox") >= 0) {
        try (var statement = connection.createStatement()) {
          var rows = statement.executeQuery("SELECT COUNT(*) FROM notification_outbox WHERE status = 'SUCCESS'");
          rows.next();
          completed = rows.getLong(1);
        }
      }
      result.put("completed", completed);
    }
    return result;
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
  }
}
