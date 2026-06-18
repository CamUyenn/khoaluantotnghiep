package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoctorMedicineResponse {

    private Long id;
    private String medicineName;
    private String medicineType;
    private String unit;
    private BigDecimal sellingPrice;
    private Integer stockQuantity;
    private Boolean isActive;
}