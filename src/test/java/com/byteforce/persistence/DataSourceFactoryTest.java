package com.byteforce.persistence;

import com.byteforce.config.AppConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSourceFactoryTest {

    @Test
    @DisplayName("Should initialize HikariDataSource with configured pool parameters")
    void shouldInitializeDataSourceWithConfiguredProperties() throws SQLException {
        AppConfig testConfig = AppConfig.load("/application-test.properties", key -> null);

        HikariDataSource dataSource = DataSourceFactory.createDataSource(testConfig);
        try {
            assertNotNull(dataSource);
            assertEquals("ByteForce-TestPool", dataSource.getPoolName());
            assertEquals(5, dataSource.getMaximumPoolSize());
            assertEquals(1, dataSource.getMinimumIdle());
            assertFalse(dataSource.isClosed());

            // Validate that we can acquire an active connection
            try (Connection connection = dataSource.getConnection()) {
                assertNotNull(connection);
                assertTrue(connection.isValid(2));

                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
        } finally {
            DataSourceFactory.close(dataSource);
            assertTrue(dataSource.isClosed());
        }
    }

    @Test
    @DisplayName("close() and closeQuietly() should safely close open pools without exceptions")
    void shouldSafelyCloseDataSource() {
        AppConfig testConfig = AppConfig.load("/application-test.properties", key -> null);
        HikariDataSource dataSource = DataSourceFactory.createDataSource(testConfig);

        assertFalse(dataSource.isClosed());

        DataSourceFactory.close(dataSource);
        assertTrue(dataSource.isClosed());

        // Calling close on already-closed or null data sources must not throw
        DataSourceFactory.close(dataSource);
        DataSourceFactory.closeQuietly(dataSource);
        DataSourceFactory.closeQuietly(null);
    }
}
