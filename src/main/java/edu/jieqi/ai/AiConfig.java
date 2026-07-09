package edu.jieqi.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AiConfig {
    private static final Path DEFAULT_FILE = Path.of("config", "ai.properties");

    private final Properties properties = new Properties();

    public static AiConfig loadDefault() {
        AiConfig config = new AiConfig();
        if (!Files.isRegularFile(DEFAULT_FILE)) {
            return config;
        }
        try (InputStream input = Files.newInputStream(DEFAULT_FILE)) {
            config.properties.load(input);
        } catch (IOException ignored) {
            // Keep defaults if the optional tuning file is unavailable.
        }
        return config;
    }

    public int intValue(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public long longValue(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public int scale(String key, int score) {
        int percent = intValue(key, 100);
        return score * percent / 100;
    }
}
