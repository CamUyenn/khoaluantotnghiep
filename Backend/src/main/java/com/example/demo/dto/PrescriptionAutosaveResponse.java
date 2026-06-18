package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrescriptionAutosaveResponse {

    private Long medicalRecordId;
    private Long medicineId;
    private Integer quantity;
    private String usageInstructions;
    private BigDecimal lineTotal;
    private BigDecimal totalAmount;
    private Integer remainingStock;
}