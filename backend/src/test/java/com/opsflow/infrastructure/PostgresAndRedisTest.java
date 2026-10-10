package com.opsflow.infrastructure;

import com.opsflow.knowledge.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The PostgreSQL profile against the real things: Flyway builds the schema, Hibernate validates
 * the entities against it, runbook search goes through pgvector, and the rate limiter counts
 * in Redis. Skipped on a machine without Docker; it runs in CI.
 */
@SpringBootTest(properties = {
        "opsflow.rate-limit.enabled=true",
        "opsflow.rate-limit.login-per-minute=5",
        "management.health.redis.enabled=true"
})
@ActiveProfiles("prod")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgresAndRedisTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private VectorStoreService search;
    @Autowired
    private MockMvc mvc;

    @Test
    void flywayBuiltTheSchemaAndTheSeedDataWentIn() {
        Integer migrations = jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
        assertEquals(2, migrations);
        Integer users = jdbc.queryForObject("select count(*) from users", Integer.class);
        assertTrue(users >= 5);
    }

    @Test
    void everySeededRunbookChunkHasAnEmbeddingInPgvector() {
        Integer missing = jdbc.queryForObject("select count(*) from knowledge_chunks where embedding is null", Integer.class);
        assertEquals(0, missing);
    }

    @Test
    void runbookSearchRanksByCosineDistanceInTheDatabase() {
        List<VectorStoreService.SearchResult> results = search.searchVectorStore("payment deducted but order status still pending", 3);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDocumentTitle().contains("Payment"), "got: " + results.get(0).getDocumentTitle());
    }

    @Test
    void theSixthLoginInAMinuteFromOneAddressIsTurnedAway() throws Exception {
        String wrongPassword = "{\"username\":\"agent\",\"password\":\"wrong\"}";
        for (int attempt = 1; attempt <= 5; attempt++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrongPassword)
                            .with(request -> { request.setRemoteAddr("203.0.113.9"); return request; }))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrongPassword)
                        .with(request -> { request.setRemoteAddr("203.0.113.9"); return request; }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
