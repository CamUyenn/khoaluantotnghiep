package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemSettingResponse {
    private String settingKey;
    private String settingValue;
    private String description;
    private String source;
}
