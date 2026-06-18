package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.DiagnosisMedicineRuleRequest;
import com.example.demo.dto.DiagnosisMedicineRuleResponse;
import com.example.demo.dto.DiagnosisTemplateRequest;
import com.example.demo.dto.DiagnosisTemplateResponse;
import com.example.demo.dto.MedicalCategoryRequest;
import com.example.demo.dto.MedicalCategoryResponse;
import com.example.demo.dto.SymptomServiceMappingRequest;
import com.example.demo.dto.SymptomServiceMappingResponse;
import com.example.demo.dto.SymptomTemplateRequest;
import com.example.demo.dto.SymptomTemplateResponse;
import com.example.demo.service.MasterDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/master-data")
@RequiredArgsConstructor
@Tag(name = "Admin Master Data", description = "Quan tri danh muc tu dong.")
public class AdminMasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping("/categories")
    @Operation(summary = "Danh sach nhom benh")
    public List<MedicalCategoryResponse> getCategories() {
        return masterDataService.getAllCategories();
    }

    @PostMapping("/categories")
    @Operation(summary = "Tao nhom benh")
    public MedicalCategoryResponse createCategory(@Valid @RequestBody MedicalCategoryRequest request) {
        return masterDataService.createCategory(request);
    }

    @PutMapping("/categories/{categoryId}")
    @Operation(summary = "Cap nhat nhom benh")
    public MedicalCategoryResponse updateCategory(
            @PathVariable("categoryId") Long categoryId,
            @Valid @RequestBody MedicalCategoryRequest request) {
        return masterDataService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/categories/{categoryId}")
    @Operation(summary = "Xoa nhom benh")
    public void deleteCategory(@PathVariable("categoryId") Long categoryId) {
        masterDataService.deleteCategory(categoryId);
    }

    @GetMapping("/symptoms")
    @Operation(summary = "Danh sach trieu chung theo nhom")
    public List<SymptomTemplateResponse> getSymptoms(@RequestParam("categoryId") Long categoryId) {
        return masterDataService.getSymptomsByCategory(categoryId);
    }

    @PostMapping("/symptoms")
    @Operation(summary = "Tao trieu chung")
    public SymptomTemplateResponse createSymptom(@Valid @RequestBody SymptomTemplateRequest request) {
        return masterDataService.createSymptom(request);
    }

    @PutMapping("/symptoms/{symptomId}")
    @Operation(summary = "Cap nhat trieu chung")
    public SymptomTemplateResponse updateSymptom(
            @PathVariable("symptomId") Long symptomId,
            @Valid @RequestBody SymptomTemplateRequest request) {
        return masterDataService.updateSymptom(symptomId, request);
    }

    @DeleteMapping("/symptoms/{symptomId}")
    @Operation(summary = "Xoa trieu chung")
    public void deleteSymptom(@PathVariable("symptomId") Long symptomId) {
        masterDataService.deleteSymptom(symptomId);
    }

    @GetMapping("/diagnoses")
    @Operation(summary = "Danh sach chan doan theo nhom")
    public List<DiagnosisTemplateResponse> getDiagnoses(@RequestParam("categoryId") Long categoryId) {
        return masterDataService.getDiagnosesByCategory(categoryId);
    }

    @PostMapping("/diagnoses")
    @Operation(summary = "Tao chan doan")
    public DiagnosisTemplateResponse createDiagnosis(@Valid @RequestBody DiagnosisTemplateRequest request) {
        return masterDataService.createDiagnosis(request);
    }

    @PutMapping("/diagnoses/{diagnosisId}")
    @Operation(summary = "Cap nhat chan doan")
    public DiagnosisTemplateResponse updateDiagnosis(
            @PathVariable("diagnosisId") Long diagnosisId,
            @Valid @RequestBody DiagnosisTemplateRequest request) {
        return masterDataService.updateDiagnosis(diagnosisId, request);
    }

    @DeleteMapping("/diagnoses/{diagnosisId}")
    @Operation(summary = "Xoa chan doan")
    public void deleteDiagnosis(@PathVariable("diagnosisId") Long diagnosisId) {
        masterDataService.deleteDiagnosis(diagnosisId);
    }

    @GetMapping("/diagnosis-medicine-rules")
    @Operation(summary = "Danh sach quy tac thuoc theo chan doan")
    public List<DiagnosisMedicineRuleResponse> getRules(@RequestParam("diagnosisId") Long diagnosisId) {
        return masterDataService.getRulesByDiagnosis(diagnosisId);
    }

    @PostMapping("/diagnosis-medicine-rules")
    @Operation(summary = "Tao quy tac thuoc")
    public DiagnosisMedicineRuleResponse createRule(@Valid @RequestBody DiagnosisMedicineRuleRequest request) {
        return masterDataService.createRule(request);
    }

    @PutMapping("/diagnosis-medicine-rules/{ruleId}")
    @Operation(summary = "Cap nhat quy tac thuoc")
    public DiagnosisMedicineRuleResponse updateRule(
            @PathVariable("ruleId") Long ruleId,
            @Valid @RequestBody DiagnosisMedicineRuleRequest request) {
        return masterDataService.updateRule(ruleId, request);
    }

    @DeleteMapping("/diagnosis-medicine-rules/{ruleId}")
    @Operation(summary = "Xoa quy tac thuoc")
    public void deleteRule(@PathVariable("ruleId") Long ruleId) {
        masterDataService.deleteRule(ruleId);
    }

    @GetMapping("/symptom-service-mappings")
    @Operation(summary = "Danh sach mapping dich vu")
    public List<SymptomServiceMappingResponse> getMappings(@RequestParam("symptomId") Long symptomId) {
        return masterDataService.getMappingsBySymptom(symptomId);
    }

    @PostMapping("/symptom-service-mappings")
    @Operation(summary = "Tao mapping dich vu")
    public SymptomServiceMappingResponse createMapping(@Valid @RequestBody SymptomServiceMappingRequest request) {
        return masterDataService.createMapping(request);
    }

    @DeleteMapping("/symptom-service-mappings")
    @Operation(summary = "Xoa mapping dich vu")
    public void deleteMapping(
            @RequestParam("symptomId") Long symptomId,
            @RequestParam("serviceId") Long serviceId) {
        masterDataService.deleteMapping(symptomId, serviceId);
    }
}
