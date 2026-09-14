package com.byteforce.app;

import com.byteforce.persistence.DatabaseMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppContextTest {

    private HikariDataSource dataSource;
    private AppContext appContext;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(config);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
        appContext = new AppContext(dataSource);
    }

    @AfterEach
    void tearDown() {
        if (appContext != null) {
            appContext.close();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should successfully wire and expose all repositories, services, and security components")
    void shouldWireAllComponents() {
        assertSame(dataSource, appContext.getDataSource());

        assertNotNull(appContext.getUserRepository());
        assertNotNull(appContext.getStudentProfileRepository());
        assertNotNull(appContext.getTopicRepository());
        assertNotNull(appContext.getQuestionRepository());
        assertNotNull(appContext.getBookmarkRepository());
        assertNotNull(appContext.getAttemptRepository());
        assertNotNull(appContext.getActivityRepository());

        assertNotNull(appContext.getPasswordHasher());
        assertNotNull(appContext.getUserSession());

        assertNotNull(appContext.getActivityService());
        assertNotNull(appContext.getAuthService());
        assertNotNull(appContext.getUserService());
        assertNotNull(appContext.getTopicService());
        assertNotNull(appContext.getQuestionService());
        assertNotNull(appContext.getBookmarkService());
        assertNotNull(appContext.getAttemptService());
    }

    @Test
    @DisplayName("Should validate null arguments on construction")
    void shouldValidateNullArguments() {
        assertThrows(NullPointerException.class, () -> new AppContext(null));
        assertThrows(NullPointerException.class, () -> AppContext.initialize(null));
    }

    @Test
    @DisplayName("Should safely handle close on non-managed DataSource")
    void shouldSafelyHandleClose() {
        assertDoesNotThrow(() -> appContext.close());
    }
}
