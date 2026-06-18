package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiagnosisMedicineRuleResponse {
    private Long id;
    private Long diagnosisId;
    private String diagnosisName;
    private Long medicineId;
    private String medicineName;
    private Integer minAge;
    private Integer maxAge;
    private Integer defaultQuantity;
    private String defaultUsage;
}
