package com.example.demo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AdminSystemSettingResponse;
import com.example.demo.entity.SystemSetting;
import com.example.demo.exception.AppException;
import com.example.demo.repository.SystemSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SystemSettingService {

    private static final Pattern KEY_PATTERN = Pattern.compile("[a-zA-Z0-9._-]{1,120}");
    private static final String SOURCE_DATABASE = "DATABASE";
    private static final String SOURCE_PROPERTIES = "APPLICATION_PROPERTIES";
    private static final String SOURCE_EMPTY = "EMPTY";

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private static final List<String> DEFAULT_ADMIN_SETTING_KEYS = List.of(
            "spring.mail.host",
            "spring.mail.port",
            "spring.mail.username",
            "spring.mail.password",
            "clinic.notification.receptionist.emails",
            "APP_LOG_MAX_FILE_SIZE",
            "APP_LOG_MAX_HISTORY_DAYS");

    private static final Map<String, String> DEFAULT_SETTING_DESCRIPTIONS = Map.of(
            "spring.mail.host", "Địa chỉ máy chủ SMTP dùng để gửi email.",
            "spring.mail.port", "Cổng SMTP của máy chủ email (ví dụ: 587).",
            "spring.mail.username", "Tài khoản email gửi đi của hệ thống.",
            "spring.mail.password", "Mật khẩu ứng dụng của tài khoản email gửi đi.",
            "clinic.notification.receptionist.emails",
            "Danh sách email lễ tân nhận thông báo, phân tách bằng dấu phẩy.",
            "APP_LOG_MAX_FILE_SIZE", "Kích thước tối đa cho mỗi file log (ví dụ: 10MB).",
            "APP_LOG_MAX_HISTORY_DAYS", "Số ngày tối đa giữ lại log file (ví dụ: 14).");

    private final SystemSettingRepository systemSettingRepository;
    private final Environment environment;

    // Chức năng: lấy tất cả cấu hình key-value cho màn hình quản trị.
    public List<AdminSystemSettingResponse> getAllSettings() {
        Map<String, SystemSetting> databaseSettings = new LinkedHashMap<>();
        for (SystemSetting item : systemSettingRepository.findAllByOrderBySettingKeyAsc()) {
            databaseSettings.put(item.getSettingKey(), item);
        }

        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(DEFAULT_ADMIN_SETTING_KEYS);
        allKeys.addAll(databaseSettings.keySet());

        List<AdminSystemSettingResponse> result = new ArrayList<>();
        for (String key : allKeys) {
            result.add(toResponse(key, databaseSettings.get(key)));
        }

        return result;
    }

    // Chức năng: lấy một cấu hình theo key.
    public AdminSystemSettingResponse getSetting(String settingKey) {
        String normalizedKey = normalizeSettingKey(settingKey);
        SystemSetting databaseSetting = systemSettingRepository.findById(normalizedKey).orElse(null);

        return toResponse(normalizedKey, databaseSetting);
    }

    // Chức năng: tạo mới hoặc cập nhật cấu hình key-value.
    @Transactional
    public AdminSystemSettingResponse upsertSetting(String settingKey, String settingValue, String description) {
        String normalizedKey = normalizeSettingKey(settingKey);
        String normalizedValue = normalizeSettingValue(settingValue);

        Optional<SystemSetting> existingSetting = systemSettingRepository.findById(normalizedKey);
        String resolvedDescription = description == null
                ? existingSetting
                        .map(SystemSetting::getDescription)
                        .filter(value -> value != null && !value.isBlank())
                        .orElseGet(() -> inferDefaultDescription(normalizedKey))
                : normalizeSettingDescription(description, normalizedKey);

        SystemSetting toSave = existingSetting.orElseGet(SystemSetting::new);
        toSave.setSettingKey(normalizedKey);
        toSave.setSettingValue(normalizedValue);
        toSave.setDescription(resolvedDescription);

        SystemSetting saved = systemSettingRepository.save(toSave);
        return new AdminSystemSettingResponse(
                saved.getSettingKey(),
                saved.getSettingValue(),
                saved.getDescription(),
                SOURCE_DATABASE);
    }

    // Chức năng: lấy cấu hình ưu tiên database, fallback về application.properties.
    public String resolveSettingValue(String settingKey, String fallbackValue) {
        String normalizedKey = normalizeSettingKey(settingKey);

        return systemSettingRepository.findById(normalizedKey)
                .map(SystemSetting::getSettingValue)
                .orElseGet(() -> {
                    String valueFromProperties = environment.getProperty(normalizedKey);
                    if (valueFromProperties != null) {
                        return valueFromProperties;
                    }
                    return fallbackValue == null ? "" : fallbackValue;
                });
    }

    private AdminSystemSettingResponse toResponse(String settingKey, SystemSetting databaseSetting) {
        if (databaseSetting != null) {
            String description = normalizeSettingDescription(
                    databaseSetting.getDescription(),
                    settingKey);
            return new AdminSystemSettingResponse(
                    settingKey,
                    databaseSetting.getSettingValue(),
                    description,
                    SOURCE_DATABASE);
        }

        String valueFromProperties = environment.getProperty(settingKey);
        String inferredDescription = inferDefaultDescription(settingKey);
        if (valueFromProperties != null) {
            return new AdminSystemSettingResponse(settingKey, valueFromProperties, inferredDescription,
                    SOURCE_PROPERTIES);
        }

        return new AdminSystemSettingResponse(settingKey, "", inferredDescription, SOURCE_EMPTY);
    }

    private String normalizeSettingKey(String settingKey) {
        if (settingKey == null || settingKey.isBlank()) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Khóa cấu hình là bắt buộc");
        }

        String normalized = settingKey.trim();
        if (!KEY_PATTERN.matcher(normalized).matches()) {
            throw AppException.of(HttpStatus.BAD_REQUEST,
                    "Khóa cấu hình chỉ chứa chữ, số và ký tự . _ - (tối đa 120 ký tự)");
        }

        return normalized;
    }

    private String normalizeSettingValue(String settingValue) {
        if (settingValue == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Giá trị cấu hình là bắt buộc");
        }

        String normalized = settingValue.trim();
        if (normalized.length() > 4000) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Giá trị cấu hình tối đa 4000 ký tự");
        }

        return normalized;
    }

    private String normalizeSettingDescription(String description, String settingKey) {
        if (description == null) {
            return inferDefaultDescription(settingKey);
        }

        String normalized = description.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Mô tả cấu hình tối đa 500 ký tự");
        }

        return normalized;
    }

    private String inferDefaultDescription(String settingKey) {
        return DEFAULT_SETTING_DESCRIPTIONS.getOrDefault(settingKey, "");
    }
}
