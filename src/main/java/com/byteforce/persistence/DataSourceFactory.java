package com.byteforce.persistence;

import com.byteforce.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Factory for creating and managing HikariCP connection pools.
 */
public final class DataSourceFactory {

    private static final Logger log = LoggerFactory.getLogger(DataSourceFactory.class);

    private DataSourceFactory() {
    }

    /**
     * Creates and initializes a HikariDataSource based on the provided AppConfig.
     */
    public static HikariDataSource createDataSource(AppConfig config) {
        Objects.requireNonNull(config, "AppConfig must not be null");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName(config.getDbPoolName());
        hikariConfig.setJdbcUrl(config.getDbUrl());
        hikariConfig.setUsername(config.getDbUsername());
        hikariConfig.setPassword(config.getDbPassword());

        hikariConfig.setMaximumPoolSize(config.getDbPoolMaxSize());
        hikariConfig.setMinimumIdle(config.getDbPoolMinIdle());
        hikariConfig.setIdleTimeout(config.getDbPoolIdleTimeoutMs());
        hikariConfig.setMaxLifetime(config.getDbPoolMaxLifetimeMs());
        hikariConfig.setConnectionTimeout(config.getDbPoolConnectionTimeoutMs());

        // Performance optimizations specifically for MySQL JDBC driver
        if (config.getDbUrl() != null && config.getDbUrl().startsWith("jdbc:mysql")) {
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        }

        log.info("Initializing HikariDataSource pool '{}' for URL: {}", config.getDbPoolName(), config.getDbUrl());
        return new HikariDataSource(hikariConfig);
    }

    /**
     * Safely closes a DataSource if it is an open HikariDataSource.
     */
    public static void close(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            if (!hikariDataSource.isClosed()) {
                String poolName = hikariDataSource.getPoolName();
                log.info("Closing HikariDataSource pool '{}'", poolName);
                hikariDataSource.close();
                log.info("HikariDataSource pool '{}' closed successfully", poolName);
            }
        }
    }

    /**
     * Closes the DataSource without throwing exceptions.
     */
    public static void closeQuietly(DataSource dataSource) {
        try {
            close(dataSource);
        } catch (Exception e) {
            log.warn("Error while closing DataSource", e);
        }
    }
}
