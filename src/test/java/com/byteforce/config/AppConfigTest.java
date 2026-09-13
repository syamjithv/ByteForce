package com.byteforce.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    @DisplayName("Should load default configuration properties from classpath")
    void shouldLoadDefaultClasspathProperties() {
        AppConfig config = AppConfig.load();

        assertNotNull(config);
        assertEquals("ByteForce", config.getAppName());
        assertNotNull(config.getDbUrl());
        assertNotNull(config.getDbUsername());
        assertEquals("ByteForce-HikariPool", config.getDbPoolName());
        assertEquals(10, config.getDbPoolMaxSize());
        assertEquals(2, config.getDbPoolMinIdle());
        assertTrue(config.isFlywayEnabled());
        assertEquals("classpath:db/migration", config.getFlywayLocations());
    }

    @Test
    @DisplayName("Should load test properties file correctly")
    void shouldLoadTestPropertiesFile() {
        AppConfig config = AppConfig.load("/application-test.properties", key -> null);

        assertEquals("ByteForce-Test", config.getAppName());
        assertEquals("0.1.0-TEST", config.getAppVersion());
        assertEquals("ByteForce-TestPool", config.getDbPoolName());
        assertEquals("sa", config.getDbUsername());
        assertEquals(5, config.getDbPoolMaxSize());
    }

    @Test
    @DisplayName("Environment variables should override properties file")
    void environmentVariablesShouldOverrideProperties() {
        Map<String, String> mockEnv = Map.of(
                "BYTEFORCE_DB_URL", "jdbc:mysql://custom-host:3306/custom_db",
                "BYTEFORCE_DB_USERNAME", "custom_admin",
                "BYTEFORCE_DB_PASSWORD", "secret123",
                "BYTEFORCE_DB_POOL_MAXIMUM_POOL_SIZE", "25"
        );

        AppConfig config = AppConfig.load(AppConfig.DEFAULT_PROPERTIES_FILE, mockEnv::get);

        assertEquals("jdbc:mysql://custom-host:3306/custom_db", config.getDbUrl());
        assertEquals("custom_admin", config.getDbUsername());
        assertEquals("secret123", config.getDbPassword());
        assertEquals(25, config.getDbPoolMaxSize());
    }

    @Test
    @DisplayName("Generic environment variable key should take effect if prefixed key is absent")
    void genericEnvironmentVariableShouldOverride() {
        Map<String, String> mockEnv = Map.of(
                "DB_URL", "jdbc:mysql://generic-host:3306/db",
                "DB_USERNAME", "generic_user"
        );

        AppConfig config = AppConfig.load(AppConfig.DEFAULT_PROPERTIES_FILE, mockEnv::get);

        assertEquals("jdbc:mysql://generic-host:3306/db", config.getDbUrl());
        assertEquals("generic_user", config.getDbUsername());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when db.url is missing")
    void shouldThrowWhenDbUrlMissing() {
        Properties props = new Properties();
        props.setProperty(AppConfig.KEY_DB_USERNAME, "root");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> AppConfig.from(props, key -> null));

        assertTrue(ex.getMessage().contains("db.url"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when db.username is missing")
    void shouldThrowWhenDbUsernameMissing() {
        Properties props = new Properties();
        props.setProperty(AppConfig.KEY_DB_URL, "jdbc:mysql://localhost:3306/byteforce");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> AppConfig.from(props, key -> null));

        assertTrue(ex.getMessage().contains("db.username"));
    }

    @Test
    @DisplayName("Should parse integer, long, and boolean properties with fallback defaults")
    void shouldParseTypesWithFallbackDefaults() {
        Properties props = new Properties();
        props.setProperty(AppConfig.KEY_DB_URL, "jdbc:mysql://localhost/test");
        props.setProperty(AppConfig.KEY_DB_USERNAME, "root");
        props.setProperty("custom.int", "42");
        props.setProperty("custom.bad.int", "not-a-number");
        props.setProperty("custom.bool", "true");

        AppConfig config = AppConfig.from(props, key -> null);

        assertEquals(42, config.getIntProperty("custom.int", 10));
        assertEquals(10, config.getIntProperty("custom.bad.int", 10));
        assertEquals(99, config.getIntProperty("custom.missing", 99));

        assertEquals(42L, config.getLongProperty("custom.int", 10L));
        assertEquals(50L, config.getLongProperty("custom.bad.int", 50L));

        assertTrue(config.getBooleanProperty("custom.bool", false));
        assertFalse(config.getBooleanProperty("custom.missing.bool", false));
    }

    @Test
    @DisplayName("toString() must protect sensitive password values")
    void toStringMustProtectSensitiveValues() {
        Properties props = new Properties();
        props.setProperty(AppConfig.KEY_DB_URL, "jdbc:mysql://localhost/test");
        props.setProperty(AppConfig.KEY_DB_USERNAME, "root");
        props.setProperty(AppConfig.KEY_DB_PASSWORD, "superSecretPassword987");

        AppConfig config = AppConfig.from(props, key -> null);
        String asString = config.toString();

        assertFalse(asString.contains("superSecretPassword987"), "Password must never appear in toString");
        assertTrue(asString.contains("[PROTECTED]"));
    }
}
