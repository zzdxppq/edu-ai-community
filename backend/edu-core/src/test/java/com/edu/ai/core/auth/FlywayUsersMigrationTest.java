package com.edu.ai.core.auth;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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
 * 1.2-INT-001 — Verify V1__create_users.sql materializes the users table,
 * default user_type, and both indexes. Mirrors the graceful-skip pattern
 * from 1.1 FlywayMigrationTest (disabled when Docker is unavailable).
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayUsersMigrationTest {

    private static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("edu_core")
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
    void test_1_2_INT_001_usersTableAndIndexesCreated() throws Exception {
        DataSource ds = new DriverManagerDataSource(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();

        MigrationInfo applied = flyway.info().applied()[0];
        assertThat(applied.getVersion().getVersion()).isEqualTo("1");
        assertThat(applied.getState().isApplied()).isTrue();

        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement()) {

            try (ResultSet rs = st.executeQuery(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema='public' AND table_name='users'")) {
                assertThat(rs.next()).as("users table must exist").isTrue();
            }

            try (ResultSet rs = st.executeQuery(
                    "SELECT indexname FROM pg_indexes WHERE tablename='users'")) {
                Set<String> indexes = new HashSet<>();
                while (rs.next()) {
                    indexes.add(rs.getString(1));
                }
                assertThat(indexes).contains("idx_users_phone", "idx_users_role");
            }

            st.executeUpdate("INSERT INTO users (phone, username, role) "
                    + "VALUES ('13800138000', '张三', 'MEMBER_RURAL')");
            try (ResultSet rs = st.executeQuery(
                    "SELECT user_type FROM users WHERE phone='13800138000'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("USER");
            }
        }
    }
}
