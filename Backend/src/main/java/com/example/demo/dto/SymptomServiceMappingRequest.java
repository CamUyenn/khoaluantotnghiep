package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SymptomServiceMappingRequest {
    @NotNull(message = "symptomId là bắt buộc")
    private Long symptomId;

    @NotNull(message = "serviceId là bắt buộc")
    private Long serviceId;
}
