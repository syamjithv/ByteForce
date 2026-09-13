package com.byteforce.persistence;

import com.byteforce.config.AppConfig;
import com.byteforce.exception.ByteForceException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Manages database schema migrations using Flyway.
 */
public final class DatabaseMigrator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrator.class);

    private DatabaseMigrator() {
    }

    /**
     * Executes database migrations using settings from AppConfig.
     *
     * @param dataSource the active DataSource connection pool
     * @param config the application configuration
     * @return number of migrations executed
     */
    public static int migrate(DataSource dataSource, AppConfig config) {
        Objects.requireNonNull(dataSource, "DataSource must not be null");
        Objects.requireNonNull(config, "AppConfig must not be null");

        if (!config.isFlywayEnabled()) {
            log.info("Flyway migrations are disabled by configuration.");
            return 0;
        }

        String locations = config.getFlywayLocations();
        return migrate(dataSource, locations.split(","));
    }

    /**
     * Executes database migrations for specified classpath/filesystem locations.
     *
     * @param dataSource the active DataSource connection pool
     * @param locations Flyway migration script locations (e.g. "classpath:db/migration")
     * @return number of migrations executed
     */
    public static int migrate(DataSource dataSource, String... locations) {
        Objects.requireNonNull(dataSource, "DataSource must not be null");
        if (locations == null || locations.length == 0) {
            locations = new String[]{"classpath:db/migration"};
        }

        log.info("Starting Flyway database migration using locations: {}", (Object) locations);

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(locations)
                    .baselineOnMigrate(true)
                    .cleanDisabled(true)
                    .load();

            MigrateResult result = flyway.migrate();
            int migrationsExecuted = result.migrationsExecuted;
            log.info("Flyway migration completed successfully. Migrations applied: {}, Target schema version: {}",
                    migrationsExecuted, result.targetSchemaVersion);
            return migrationsExecuted;
        } catch (FlywayException e) {
            log.error("Database migration failed: {}", e.getMessage(), e);
            throw new ByteForceException("Database migration failed: " + e.getMessage(), e);
        }
    }
}
