package com.edu.ai.intelligence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@code edu-ai} Flyway baseline.
 *
 * <p>Test Design: docs/qa/assessments/1.1-test-design-20260422.md
 * <ul>
 *   <li>1.1-INT-002 (P1): V1__init_extensions.sql enables pgvector (and zhparser where available).</li>
 *   <li>1.1-INT-003 (P2): Second migrate call is a no-op — Flyway is idempotent.</li>
 * </ul>
 *
 * <p>Runs against the {@code pgvector/pgvector:pg16} container (superuser, includes
 * the vector extension). The zhparser extension is NOT in this image; the migration's
 * {@code DO $$ ... EXCEPTION WHEN undefined_file} block tolerates its absence so this
 * test asserts the presence of {@code vector} only. Production environments provision
 * a custom image with zhparser pre-installed (see
 * {@code docs/fullstack-architecture/database-schema.md}).
 *
 * <p>{@code disabledWithoutDocker = true} ensures the class is skipped cleanly in
 * environments that lack Docker access (e.g. unprivileged CI runners). The local
 * developer workflow provisions Docker via {@code scripts/dev-up.sh}.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

    private static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("edu_ai")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @BeforeAll
    static void startContainer() {
        PG.start();
    }

    @AfterAll
    static void stopContainer() {
        PG.stop();
    }

    @Test
    void test_1_1_INT_002_flywayEnablesExtensions() throws Exception {
        Flyway flyway = newFlyway();
        flyway.migrate();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT extname FROM pg_extension WHERE extname IN ('vector','zhparser')")) {

            Set<String> present = new HashSet<>();
            while (rs.next()) {
                present.add(rs.getString(1));
            }
            assertThat(present)
                    .as("V1__init_extensions.sql must enable pgvector in the pgvector:pg16 image")
                    .contains("vector");
            // zhparser is image-dependent; the migration tolerates its absence by design.
        }

        MigrationInfo applied = flyway.info().applied()[0];
        assertThat(applied.getVersion().getVersion()).isEqualTo("1");
        assertThat(applied.getDescription()).containsIgnoringCase("init extensions");
    }

    @Test
    void test_1_1_INT_003_flywayIdempotent() {
        Flyway flyway = newFlyway();
        MigrateResult first = flyway.migrate();
        int firstApplied = first.migrationsExecuted;

        MigrateResult second = flyway.migrate();
        assertThat(second.migrationsExecuted)
                .as("second migrate() must be a no-op when baseline is already applied")
                .isZero();

        // Schema history row count should not grow beyond what the first migrate added.
        assertThat(flyway.info().applied()).hasSize(Math.max(firstApplied, 1));
    }

    private Flyway newFlyway() {
        DataSource ds = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        return Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
