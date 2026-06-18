package com.example.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {

	private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/KLTN";
	private static final String DEFAULT_DB_USERNAME = "postgres";
	private static final String DEFAULT_DB_PASSWORD = "123456";

	private static final Map<String, String> LOG_SETTING_TO_SYSTEM_PROPERTY = Map.of(
			"APP_LOG_MAX_FILE_SIZE", "APP_LOG_MAX_FILE_SIZE",
			"APP_LOG_MAX_HISTORY_DAYS", "APP_LOG_MAX_HISTORY_DAYS",
			"logging.logback.rollingpolicy.max-file-size", "APP_LOG_MAX_FILE_SIZE",
			"logging.logback.rollingpolicy.max-history", "APP_LOG_MAX_HISTORY_DAYS");

	// Chức năng: xử lý main.
	public static void main(String[] args) {
		preloadLogSettingsFromDatabase();
		SpringApplication.run(DemoApplication.class, args);
	}

	private static void preloadLogSettingsFromDatabase() {
		String jdbcUrl = readConfigValue("DB_URL", DEFAULT_DB_URL);
		String username = readConfigValue("DB_USERNAME", DEFAULT_DB_USERNAME);
		String password = readConfigValue("DB_PASSWORD", DEFAULT_DB_PASSWORD);

		String sql = """
				SELECT setting_key, setting_value
				FROM system_settings
				WHERE setting_key IN (?, ?, ?, ?)
				""";

		try {
			Class.forName("org.postgresql.Driver");
			try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
					PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setString(1, "APP_LOG_MAX_FILE_SIZE");
				statement.setString(2, "APP_LOG_MAX_HISTORY_DAYS");
				statement.setString(3, "logging.logback.rollingpolicy.max-file-size");
				statement.setString(4, "logging.logback.rollingpolicy.max-history");

				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						String settingKey = resultSet.getString("setting_key");
						String settingValue = resultSet.getString("setting_value");
						String targetProperty = LOG_SETTING_TO_SYSTEM_PROPERTY.get(settingKey);

						if (targetProperty == null || settingValue == null || settingValue.isBlank()) {
							continue;
						}

						if (!isExplicitlyConfigured(targetProperty)) {
							System.setProperty(targetProperty, settingValue.trim());
						}
					}
				}
			}
		} catch (Exception ex) {
			System.err.println("[Bootstrap] Skip loading log settings from DB: " + ex.getMessage());
		}
	}

	private static String readConfigValue(String key, String defaultValue) {
		String fromSystemProperty = System.getProperty(key);
		if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
			return fromSystemProperty.trim();
		}

		String fromEnv = System.getenv(key);
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv.trim();
		}

		return defaultValue;
	}

	private static boolean isExplicitlyConfigured(String propertyName) {
		String fromSystemProperty = System.getProperty(propertyName);
		if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
			return true;
		}

		String fromEnv = System.getenv(propertyName);
		return fromEnv != null && !fromEnv.isBlank();
	}

}
