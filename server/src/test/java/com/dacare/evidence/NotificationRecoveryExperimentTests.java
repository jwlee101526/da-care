package com.dacare.evidence;

import static org.junit.jupiter.api.Assertions.*;

import com.dacare.server.service.ReservationService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 알림 발송 시점의 프로세스 종료와 HTTP 실패 상황에서 테스트 전용 Outbox 프로토타입의 복구 동작을 검증한다.
 */
class NotificationRecoveryExperimentTests {

  @Test
  @Timeout(180)
  void existingImplementationLosesNotificationAfterProcessCrash() throws Exception {
    NotificationCrashEvidence.main(new String[]{"before"});
  }

  @Test
  @Timeout(180)
  void testOnlyOutboxRecoversNotificationAfterProcessRestart() throws Exception {
    NotificationCrashEvidence.main(new String[]{"after"});
  }

  @Test
  void prototypeRollsBackRequestTogetherWithReservation() {
    try (var context = NotificationCrashEvidence.startContext("crash", memoryDatabase(), "http://127.0.0.1:1", "after")) {
      var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
      assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
        context.getBean(ReservationService.class).createGuest("computer", "가상 증상", "테스트 주소",
            LocalDateTime.now().plusDays(2), "검증용 고객", "01000000000", null);
        throw new IllegalStateException("예약 롤백 주입");
      }));
      var jdbc = context.getBean(JdbcTemplate.class);
      assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM reservation", Integer.class));
      assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox", Integer.class));
    }
  }

  @Test
  void prototypeKeepsRequestWhenHttpCallFailsAndRetriesLater() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      exchange.getRequestBody().readAllBytes();
      exchange.sendResponseHeaders(calls.incrementAndGet() == 1 ? 503 : 200, -1);
      exchange.close();
    });
    server.start();
    try (var context = NotificationCrashEvidence.startContext("crash", memoryDatabase(),
        "http://127.0.0.1:" + server.getAddress().getPort(), "after")) {
      context.getBean(ReservationService.class).createGuest("computer", "가상 증상", "테스트 주소",
          LocalDateTime.now().plusDays(2), "검증용 고객", "01000000000", null);
      var worker = context.getBean(OutboxPrototype.class);
      var jdbc = context.getBean(JdbcTemplate.class);
      worker.processPending();
      assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM notification_outbox", String.class));
      assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM reservation", Integer.class));
      worker.processPending();
      assertEquals("SUCCESS", jdbc.queryForObject("SELECT status FROM notification_outbox", String.class));
      assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification_history", Integer.class));
      assertEquals(2, calls.get());
    } finally {
      server.stop(0);
    }
  }

  private String memoryDatabase() {
    return "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
  }
}
