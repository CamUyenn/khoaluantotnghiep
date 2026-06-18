package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.DiagnosisTemplateResponse;
import com.example.demo.dto.MedicalCategoryResponse;
import com.example.demo.dto.SymptomTemplateResponse;
import com.example.demo.service.MasterDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Danh muc cho benh nhan va bac si.")
public class CatalogController {

    private final MasterDataService masterDataService;

    @GetMapping("/categories")
    @Operation(summary = "Danh sach nhom benh")
    public List<MedicalCategoryResponse> getCategories() {
        return masterDataService.getActiveCategories();
    }

    @GetMapping("/categories/{categoryId}/symptoms")
    @Operation(summary = "Danh sach trieu chung theo nhom")
    public List<SymptomTemplateResponse> getSymptoms(@PathVariable("categoryId") Long categoryId) {
        return masterDataService.getSymptomsByCategory(categoryId);
    }

    @GetMapping("/categories/{categoryId}/diagnoses")
    @Operation(summary = "Danh sach chan doan theo nhom")
    public List<DiagnosisTemplateResponse> getDiagnoses(@PathVariable("categoryId") Long categoryId) {
        return masterDataService.getDiagnosesByCategory(categoryId);
    }
}
