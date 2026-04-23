package com.edu.ai.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Startup self-check: verifies that PostgreSQL extensions required by later
 * RAG / full-text search stories are present.
 *
 * <p>Behavior (Story 1.1 T2 / BLIND-ERROR-003):
 * missing extensions emit a WARN log but do NOT block startup. Extension
 * provisioning is handled by the {@code edu-ai} Flyway migration
 * {@code V1__init_extensions.sql}; this service merely surfaces misconfiguration.
 */
@Component
public class PgExtensionSelfCheck {

    private static final Logger log = LoggerFactory.getLogger(PgExtensionSelfCheck.class);
    private static final Set<String> REQUIRED = Set.of("vector", "zhparser");

    private final JdbcTemplate jdbc;

    public PgExtensionSelfCheck(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifyExtensions() {
        check(jdbc);
    }

    /**
     * Exposed for direct unit testing with a mocked {@link JdbcTemplate}.
     */
    public static void check(JdbcTemplate jdbc) {
        try {
            List<String> present = jdbc.queryForList(
                    "SELECT extname FROM pg_extension WHERE extname IN ('vector','zhparser')",
                    String.class);
            for (String name : REQUIRED) {
                if (!present.contains(name)) {
                    log.warn("pgvector/zhparser self-check: extension '{}' not enabled — "
                            + "dependent RAG features will be degraded.", name);
                }
            }
        } catch (Exception ex) {
            log.warn("pgvector/zhparser self-check failed: {}", ex.getMessage());
        }
    }
}
