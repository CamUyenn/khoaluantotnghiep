package com.example.demo.config.seeder;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.SystemSetting;
import com.example.demo.repository.SystemSettingRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SystemSettingSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemSettingSeeder.class);

    @Value("${app.seed.system-settings.enabled:true}")
    private boolean defaultSystemSettingsEnabled;

    private final SystemSettingRepository systemSettingRepository;
    private final Environment environment;

    @Transactional
    public void seed() {
        if (!defaultSystemSettingsEnabled) {
            return;
        }

        List<SystemSettingSeedItem> items = buildDefaultSystemSettingItems();
        int inserted = 0;
        for (SystemSettingSeedItem item : items) {
            if (systemSettingRepository.findById(item.settingKey()).isPresent()) {
                continue;
            }

            SystemSetting setting = new SystemSetting();
            setting.setSettingKey(item.settingKey());
            setting.setSettingValue(item.settingValue());
            setting.setDescription(item.description());
            systemSettingRepository.save(setting);
            inserted++;
        }

        LOGGER.info("[SystemSettingSeeder] Seeded system settings: {} newly inserted", inserted);
    }

    private List<SystemSettingSeedItem> buildDefaultSystemSettingItems() {
        List<SystemSettingSeedItem> items = new ArrayList<>();
        items.add(new SystemSettingSeedItem(
                "spring.mail.host",
                resolveEnvValue("spring.mail.host", "smtp.gmail.com"),
                "SMTP host for outbound mail"));
        items.add(new SystemSettingSeedItem(
                "spring.mail.port",
                resolveEnvValue("spring.mail.port", "587"),
                "SMTP port for outbound mail"));
        items.add(new SystemSettingSeedItem(
                "spring.mail.username",
                resolveEnvValue("spring.mail.username", ""),
                "SMTP username"));
        items.add(new SystemSettingSeedItem(
                "spring.mail.password",
                resolveEnvValue("spring.mail.password", ""),
                "SMTP password"));
        items.add(new SystemSettingSeedItem(
                "clinic.notification.receptionist.emails",
                resolveEnvValue("clinic.notification.receptionist.emails", ""),
                "Receptionist notification emails"));
        items.add(new SystemSettingSeedItem(
                "APP_LOG_MAX_FILE_SIZE",
                resolveEnvValue("APP_LOG_MAX_FILE_SIZE", "10MB"),
                "Max log file size"));
        items.add(new SystemSettingSeedItem(
                "APP_LOG_MAX_HISTORY_DAYS",
                resolveEnvValue("APP_LOG_MAX_HISTORY_DAYS", "14"),
                "Max log history days"));
        return items;
    }

    private String resolveEnvValue(String key, String fallback) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private record SystemSettingSeedItem(String settingKey, String settingValue, String description) {
    }
}
