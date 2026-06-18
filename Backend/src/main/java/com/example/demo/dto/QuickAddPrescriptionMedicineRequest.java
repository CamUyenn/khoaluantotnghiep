package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuickAddPrescriptionMedicineRequest {

    @NotNull(message = "medicineId là bắt buộc")
    private Long medicineId;
}