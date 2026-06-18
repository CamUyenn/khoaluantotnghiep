package com.example.demo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertMedicalRecordServiceResultRequest {

    @NotNull
    private Long serviceId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private BigDecimal actualPrice;

    private String resultNote;
}
