package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoctorPatientPrescriptionItemResponse {

    private Long medicineId;
    private String medicineName;
    private Integer quantity;
    private String usageInstructions;
}