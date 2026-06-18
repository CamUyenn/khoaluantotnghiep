package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatientPrescriptionHistoryItemResponse {

    private Long medicineId;
    private String medicineName;
    private String unit;
    private Integer quantity;
    private String usageInstructions;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}