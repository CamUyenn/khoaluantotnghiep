package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "ai.gemini")
public class GeminiProperties {
    private boolean enabled = false;
    private String apiKey = "";
    private String model = "gemini-2.5-flash-lite";
    private String endpoint = "https://generativelanguage.googleapis.com/v1beta/models";
    private int timeoutSeconds = 20;
    private int historyContextMessages = 12;
    private int historyReturnMessages = 100;
    private String clinicWorkingHours = "Thứ 2 - Thứ 7: 08:00 - 17:00; Chủ nhật: 08:00 - 11:00";
    private String clinicHotline = "1900 1234";
    private String clinicAddress = "123 Đường Lý Thường Kiệt, phường Thuận Hóa, TP. Huế";
}
