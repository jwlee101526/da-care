package com.dacare.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dacare;MODE=MariaDB;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.mariadb.autoconfigure.MariaDbStoreAutoConfiguration",
        "spring.ai.openai.api-key=test-key",
        "app.jwt.secret=test-secret-that-is-long-enough-for-hmac-signing-key",
        "app.jwt.expiration=PT8H",
        "app.admin.email=admin@test.local",
        "app.admin.password=test-admin-password",
        "app.slack.webhook-url=",
        "app.solapi.api-key=",
        "app.solapi.api-secret=",
        "app.solapi.sender=",
        "app.solapi.kakao-pf-id=",
        "app.solapi.kakao-template-id="
})
class ServerApplicationTests {

  @Test
  void contextLoads() {
  }

}
