package com.qa.autonomous.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration loader.
 * Reads from application.properties with environment variable overrides.
 * All agents and components reference this class for configuration.
 */
public class SystemConfig {

    private static final Logger log = LoggerFactory.getLogger(SystemConfig.class);
    private static final String CONFIG_FILE = "application.properties";
    private static SystemConfig instance;
    private final Properties props = new Properties();

    private SystemConfig() {
        loadProperties();
    }

    public static synchronized SystemConfig getInstance() {
        if (instance == null) {
            instance = new SystemConfig();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                log.warn("Config file '{}' not found on classpath. Using defaults.", CONFIG_FILE);
                return;
            }
            props.load(is);
            log.info("Configuration loaded from {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("Failed to load configuration: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets a property value, with environment variable fallback, then default.
     */
    public String get(String key, String defaultValue) {
        // ENV takes highest priority (CI/CD override)
        String envKey = key.toUpperCase().replace(".", "_");
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) return envVal;

        // System property next (JVM -D flags)
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) return sysProp;

        // application.properties last
        return props.getProperty(key, defaultValue);
    }

    public String get(String key) {
        return get(key, null);
    }

    public int getInt(String key, int defaultValue) {
        try {
            String val = get(key, String.valueOf(defaultValue));
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer for key '{}', using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(val.trim());
    }

    public long getLong(String key, long defaultValue) {
        try {
            String val = get(key, String.valueOf(defaultValue));
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid long for key '{}', using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    // ---- Typed accessors for frequently used keys ----

    public String getOllamaBaseUrl() {
        return get("ollama.base.url", "http://localhost:11434");
    }

    public String getOllamaModel() {
        return get("ollama.model", "llama3");
    }

    public int getOllamaTimeoutSeconds() {
        return getInt("ollama.timeout.seconds", 120);
    }

    public int getOllamaMaxTokens() {
        return getInt("ollama.max.tokens", 4096);
    }

    public double getOllamaTemperature() {
        try {
            return Double.parseDouble(get("ollama.temperature", "0.1"));
        } catch (NumberFormatException e) {
            return 0.1;
        }
    }

    public int getOrchestratorMaxRetries() {
        return getInt("orchestrator.max.retries", 3);
    }

    public long getOrchestratorRetryDelayMs() {
        return getLong("orchestrator.retry.delay.ms", 1000);
    }

    public String getPlaywrightBaseUrl() {
        return get("playwright.base.url", "http://localhost:3000");
    }

    public boolean isPlaywrightHeadless() {
        return getBoolean("playwright.headless", true);
    }

    public String getRestAssuredBaseUri() {
        return get("restassured.base.uri", "http://localhost:8080");
    }

    public String getAllureResultsDirectory() {
        return get("allure.results.directory", "target/allure-results");
    }

    public String getCodeGenOutputDir() {
        return get("codegen.output.dir", "src/test/java/com/qa/autonomous/generated");
    }

    public boolean isLogAgentPrompts() {
        return getBoolean("log.agent.prompts", true);
    }

    public boolean isLogTokenUsage() {
        return getBoolean("log.token.usage", true);
    }
}
