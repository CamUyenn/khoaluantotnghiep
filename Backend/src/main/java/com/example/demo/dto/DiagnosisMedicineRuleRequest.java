package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DiagnosisMedicineRuleRequest {
    @NotNull(message = "diagnosisId là bắt buộc")
    private Long diagnosisId;

    @NotNull(message = "medicineId là bắt buộc")
    private Long medicineId;

    @NotNull(message = "minAge là bắt buộc")
    @Min(value = 0, message = "minAge phải >= 0")
    private Integer minAge;

    @NotNull(message = "maxAge là bắt buộc")
    @Min(value = 0, message = "maxAge phải >= 0")
    private Integer maxAge;

    @NotNull(message = "defaultQuantity là bắt buộc")
    @Min(value = 1, message = "defaultQuantity phải >= 1")
    private Integer defaultQuantity;

    @Size(max = 255, message = "defaultUsage tối đa 255 ký tự")
    private String defaultUsage;
}
