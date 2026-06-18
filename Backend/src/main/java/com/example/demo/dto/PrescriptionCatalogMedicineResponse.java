package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrescriptionCatalogMedicineResponse {

    private Long medicineId;
    private String medicineName;
    private String pharmacologyGroup;
    private String concentration;
    private String unit;
    private BigDecimal sellingPrice;
    private Integer stockQuantity;
    private Boolean canQuickAdd;
    private String outOfStockMessage;
}