package com.dacare.server;

import com.dacare.server.service.AuthService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TEST_POSTGRES_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=${TEST_POSTGRES_URL}",
        "spring.datasource.username=dacare",
        "spring.datasource.password=ci-test-password",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/postgresql",
        "spring.autoconfigure.exclude=",
        "spring.ai.vectorstore.pgvector.initialize-schema=true",
        "spring.ai.vectorstore.pgvector.dimensions=1536",
        "spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE",
        "OPENAI_API_KEY=",
        "management.endpoint.health.probes.enabled=true",
        "management.endpoint.health.group.readiness.include=readinessState,db",
        "app.demo-data.enabled=false"
})
class PostgresDeploymentTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthService auth;
    @LocalServerPort int port;

    @Test
    void migrationAndVectorStoreAreUsable() {
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class));
        assertNotNull(jdbc.queryForObject("SELECT to_regclass('vector_store')::text", String.class));
        assertEquals(1536, jdbc.queryForObject(
                "SELECT vector_dims(array_fill(0::real, ARRAY[1536])::vector)", Integer.class));
        assertFalse(auth.login("admin@test.local", "test-admin-password").isBlank());
    }

    @Test
    void pagesArePublicAndPrivateApisStayProtected() throws Exception {
        var client = HttpClient.newHttpClient();
        for (String path : new String[]{"/", "/reserve", "/en/reservations", "/admin"}) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), path);
            assertTrue(response.body().contains("id=\"root\""), path);
        }
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/admin/reservations")).build(), HttpResponse.BodyHandlers.ofString());
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
        var health = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health/readiness")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("UP"));
    }
}
