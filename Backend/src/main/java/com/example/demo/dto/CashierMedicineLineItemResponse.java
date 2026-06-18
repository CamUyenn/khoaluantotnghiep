package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CashierMedicineLineItemResponse {

    private Long medicineId;
    private String medicineName;
    private Integer quantity;
    private String usageInstructions;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}