package com.byteforce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * Centralized application configuration.
 * Loads base properties from classpath and supports environment variable overrides.
 * Sensitive values such as passwords are never exposed in toString() or logs.
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    public static final String DEFAULT_PROPERTIES_FILE = "/application.properties";

    // Property keys
    public static final String KEY_APP_NAME = "app.name";
    public static final String KEY_APP_VERSION = "app.version";
    public static final String KEY_APP_THEME = "app.theme";

    public static final String KEY_DB_URL = "db.url";
    public static final String KEY_DB_USERNAME = "db.username";
    public static final String KEY_DB_PASSWORD = "db.password";

    public static final String KEY_DB_POOL_NAME = "db.pool.name";
    public static final String KEY_DB_POOL_MAX_SIZE = "db.pool.maximum-pool-size";
    public static final String KEY_DB_POOL_MIN_IDLE = "db.pool.minimum-idle";
    public static final String KEY_DB_POOL_IDLE_TIMEOUT = "db.pool.idle-timeout-ms";
    public static final String KEY_DB_POOL_MAX_LIFETIME = "db.pool.max-lifetime-ms";
    public static final String KEY_DB_POOL_CONN_TIMEOUT = "db.pool.connection-timeout-ms";

    public static final String KEY_FLYWAY_ENABLED = "flyway.enabled";
    public static final String KEY_FLYWAY_LOCATIONS = "flyway.locations";

    private final Properties properties;
    private final Function<String, String> envLookup;

    public AppConfig(Properties properties, Function<String, String> envLookup) {
        this.properties = new Properties();
        if (properties != null) {
            this.properties.putAll(properties);
        }
        this.envLookup = envLookup != null ? envLookup : System::getenv;
    }

    /**
     * Loads configuration from default classpath /application.properties with system environment overrides.
     */
    public static AppConfig load() {
        return load(DEFAULT_PROPERTIES_FILE, System::getenv);
    }

    /**
     * Loads configuration from a specified classpath resource with custom environment lookup (useful for testing).
     */
    public static AppConfig load(String resourcePath, Function<String, String> envLookup) {
        Properties props = new Properties();
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream in = AppConfig.class.getResourceAsStream(path)) {
            if (in != null) {
                props.load(in);
                log.info("Loaded configuration properties from classpath resource: {}", path);
            } else {
                log.warn("Classpath configuration resource '{}' not found. Falling back to environment variables only.", path);
            }
        } catch (IOException e) {
            log.error("Failed to read configuration resource '{}'", path, e);
            throw new IllegalStateException("Failed to load configuration from " + path, e);
        }

        AppConfig config = new AppConfig(props, envLookup);
        config.validateRequiredProperties();
        return config;
    }

    /**
     * Creates an AppConfig from custom properties and custom environment lookup (primarily for testing).
     */
    public static AppConfig from(Properties properties, Function<String, String> envLookup) {
        AppConfig config = new AppConfig(properties, envLookup);
        config.validateRequiredProperties();
        return config;
    }

    /**
     * Validates that critical configuration properties are present.
     */
    public void validateRequiredProperties() {
        String dbUrl = getDbUrl();
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException("Missing required configuration property: '" + KEY_DB_URL + "'");
        }
        String dbUser = getDbUsername();
        if (dbUser == null || dbUser.isBlank()) {
            throw new IllegalStateException("Missing required configuration property: '" + KEY_DB_USERNAME + "'");
        }
    }

    /**
     * Resolves a property value: environment variables take precedence over file properties.
     */
    public String getProperty(String key) {
        Objects.requireNonNull(key, "Property key must not be null");

        // 1. Check custom / exact environment variable (e.g. BYTEFORCE_DB_URL or DB_URL)
        String envKeyPrimary = "BYTEFORCE_" + key.replace('.', '_').replace('-', '_').toUpperCase();
        String envVal = envLookup.apply(envKeyPrimary);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }

        String envKeySecondary = key.replace('.', '_').replace('-', '_').toUpperCase();
        envVal = envLookup.apply(envKeySecondary);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }

        // 2. Check properties file
        String fileVal = properties.getProperty(key);
        return fileVal != null ? fileVal.trim() : null;
    }

    public String getProperty(String key, String defaultValue) {
        String val = getProperty(key);
        return val != null ? val : defaultValue;
    }

    public int getIntProperty(String key, int defaultValue) {
        String val = getProperty(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value '{}' for property '{}', falling back to default: {}", val, key, defaultValue);
            return defaultValue;
        }
    }

    public long getLongProperty(String key, long defaultValue) {
        String val = getProperty(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("Invalid long value '{}' for property '{}', falling back to default: {}", val, key, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String val = getProperty(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val);
    }

    // Typed Getters

    public String getAppName() {
        return getProperty(KEY_APP_NAME, "ByteForce");
    }

    public String getAppVersion() {
        return getProperty(KEY_APP_VERSION, "0.1.0-SNAPSHOT");
    }

    public String getAppTheme() {
        return getProperty(KEY_APP_THEME, "light");
    }

    public String getDbUrl() {
        return getProperty(KEY_DB_URL);
    }

    public String getDbUsername() {
        return getProperty(KEY_DB_USERNAME);
    }

    public String getDbPassword() {
        String pass = getProperty(KEY_DB_PASSWORD);
        return pass != null ? pass : "";
    }

    public String getDbPoolName() {
        return getProperty(KEY_DB_POOL_NAME, "ByteForce-HikariPool");
    }

    public int getDbPoolMaxSize() {
        return getIntProperty(KEY_DB_POOL_MAX_SIZE, 10);
    }

    public int getDbPoolMinIdle() {
        return getIntProperty(KEY_DB_POOL_MIN_IDLE, 2);
    }

    public long getDbPoolIdleTimeoutMs() {
        return getLongProperty(KEY_DB_POOL_IDLE_TIMEOUT, 600000L);
    }

    public long getDbPoolMaxLifetimeMs() {
        return getLongProperty(KEY_DB_POOL_MAX_LIFETIME, 1800000L);
    }

    public long getDbPoolConnectionTimeoutMs() {
        return getLongProperty(KEY_DB_POOL_CONN_TIMEOUT, 30000L);
    }

    public boolean isFlywayEnabled() {
        return getBooleanProperty(KEY_FLYWAY_ENABLED, true);
    }

    public String getFlywayLocations() {
        return getProperty(KEY_FLYWAY_LOCATIONS, "classpath:db/migration");
    }

    @Override
    public String toString() {
        return "AppConfig{" +
                "appName='" + getAppName() + '\'' +
                ", appVersion='" + getAppVersion() + '\'' +
                ", dbUrl='" + getDbUrl() + '\'' +
                ", dbUsername='" + getDbUsername() + '\'' +
                ", dbPassword=[PROTECTED]" +
                ", dbPoolName='" + getDbPoolName() + '\'' +
                ", dbPoolMaxSize=" + getDbPoolMaxSize() +
                ", dbPoolMinIdle=" + getDbPoolMinIdle() +
                ", flywayEnabled=" + isFlywayEnabled() +
                ", flywayLocations='" + getFlywayLocations() + '\'' +
                '}';
    }
}
