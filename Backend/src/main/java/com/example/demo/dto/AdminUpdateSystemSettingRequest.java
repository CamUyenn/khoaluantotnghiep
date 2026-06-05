package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateSystemSettingRequest {

    @NotNull(message = "Giá trị cấu hình là bắt buộc")
    @Size(max = 4000, message = "Giá trị cấu hình tối đa 4000 ký tự")
    private String settingValue;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;
}
