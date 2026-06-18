package com.example.demo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrescriptionMedicineCatalogResponse {

    private String selectedGroup;
    private List<String> availableGroups;
    private List<PrescriptionCatalogMedicineResponse> medicines;
}