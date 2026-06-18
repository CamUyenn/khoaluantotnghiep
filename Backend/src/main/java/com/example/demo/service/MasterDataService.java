package com.example.demo.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
import com.example.demo.entity.DiagnosisMedicineRule;
import com.example.demo.entity.DiagnosisTemplate;
import com.example.demo.entity.MedicalCategory;
import com.example.demo.entity.MedicalService;
import com.example.demo.entity.Medicine;
import com.example.demo.entity.SymptomServiceMapping;
import com.example.demo.entity.SymptomServiceMappingId;
import com.example.demo.entity.SymptomTemplate;
import com.example.demo.exception.AppException;
import com.example.demo.repository.DiagnosisMedicineRuleRepository;
import com.example.demo.repository.DiagnosisTemplateRepository;
import com.example.demo.repository.MedicalCategoryRepository;
import com.example.demo.repository.MedicalServiceRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.SymptomServiceMappingRepository;
import com.example.demo.repository.SymptomTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class MasterDataService {

    private final MedicalCategoryRepository medicalCategoryRepository;
    private final SymptomTemplateRepository symptomTemplateRepository;
    private final DiagnosisTemplateRepository diagnosisTemplateRepository;
    private final DiagnosisMedicineRuleRepository diagnosisMedicineRuleRepository;
    private final SymptomServiceMappingRepository symptomServiceMappingRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalServiceRepository medicalServiceRepository;

    public List<MedicalCategoryResponse> getActiveCategories() {
        return medicalCategoryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public List<MedicalCategoryResponse> getAllCategories() {
        return medicalCategoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public MedicalCategoryResponse createCategory(MedicalCategoryRequest request) {
        String name = normalizeRequiredText(request.getName(), "Tên nhóm bệnh là bắt buộc");
        if (medicalCategoryRepository.existsByNameIgnoreCase(name)) {
            throw AppException.of(HttpStatus.CONFLICT, "Tên nhóm bệnh đã tồn tại");
        }

        MedicalCategory category = new MedicalCategory();
        category.setName(name);
        category.setDescription(normalizeOptionalText(request.getDescription()));
        category.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        return toCategoryResponse(medicalCategoryRepository.save(category));
    }

    public MedicalCategoryResponse updateCategory(Long categoryId, MedicalCategoryRequest request) {
        MedicalCategory category = medicalCategoryRepository.findById(categoryId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm bệnh"));

        String name = normalizeRequiredText(request.getName(), "Tên nhóm bệnh là bắt buộc");
        medicalCategoryRepository.findAllByOrderByNameAsc().stream()
                .filter(existing -> !Objects.equals(existing.getId(), categoryId))
                .filter(existing -> existing.getName() != null
                        && existing.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(existing -> {
                    throw AppException.of(HttpStatus.CONFLICT, "Tên nhóm bệnh đã tồn tại");
                });

        category.setName(name);
        category.setDescription(normalizeOptionalText(request.getDescription()));
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }
        return toCategoryResponse(medicalCategoryRepository.save(category));
    }

    public void deleteCategory(Long categoryId) {
        MedicalCategory category = medicalCategoryRepository.findById(categoryId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm bệnh"));
        medicalCategoryRepository.delete(category);
    }

    public List<SymptomTemplateResponse> getSymptomsByCategory(Long categoryId) {
        return symptomTemplateRepository.findByCategory_IdOrderBySymptomNameAsc(categoryId)
                .stream()
                .map(this::toSymptomResponse)
                .toList();
    }

    public SymptomTemplateResponse createSymptom(SymptomTemplateRequest request) {
        MedicalCategory category = medicalCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm bệnh"));

        SymptomTemplate symptom = new SymptomTemplate();
        symptom.setCategory(category);
        symptom.setSymptomName(normalizeRequiredText(request.getSymptomName(), "Tên triệu chứng là bắt buộc"));
        return toSymptomResponse(symptomTemplateRepository.save(symptom));
    }

    public SymptomTemplateResponse updateSymptom(Long symptomId, SymptomTemplateRequest request) {
        SymptomTemplate symptom = symptomTemplateRepository.findById(symptomId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy triệu chứng"));

        MedicalCategory category = medicalCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm bệnh"));

        symptom.setCategory(category);
        symptom.setSymptomName(normalizeRequiredText(request.getSymptomName(), "Tên triệu chứng là bắt buộc"));
        return toSymptomResponse(symptomTemplateRepository.save(symptom));
    }

    public void deleteSymptom(Long symptomId) {
        SymptomTemplate symptom = symptomTemplateRepository.findById(symptomId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy triệu chứng"));
        symptomTemplateRepository.delete(symptom);
    }

    public List<DiagnosisTemplateResponse> getDiagnosesByCategory(Long categoryId) {
        return diagnosisTemplateRepository.findByCategory_IdOrderByDiagnosisNameAsc(categoryId)
                .stream()
                .map(this::toDiagnosisResponse)
                .toList();
    }

    public DiagnosisTemplateResponse createDiagnosis(DiagnosisTemplateRequest request) {
        MedicalCategory category = medicalCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm bệnh"));

        DiagnosisTemplate diagnosis = new DiagnosisTemplate();
        diagnosis.setCategory(category);
        diagnosis.setDiagnosisName(normalizeRequiredText(request.getDiagnosisName(), "Tên chẩn đoán là bắt buộc"));
        diagnosis.setDefaultAdvice(normalizeOptionalText(request.getDefaultAdvice()));
        return toDiagnosisResponse(diagnosisTemplateRepository.save(diagnosis));
    }

    public DiagnosisTemplateResponse updateDiagnosis(Long diagnosisId, DiagnosisTemplateRequest request) {
        DiagnosisTemplate diagnosis = diagnosisTemplateRepository.findById(diagnosisId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy chẩn đoán"));

        MedicalCategory category = medicalCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm bệnh"));

        diagnosis.setCategory(category);
        diagnosis.setDiagnosisName(normalizeRequiredText(request.getDiagnosisName(), "Tên chẩn đoán là bắt buộc"));
        diagnosis.setDefaultAdvice(normalizeOptionalText(request.getDefaultAdvice()));
        return toDiagnosisResponse(diagnosisTemplateRepository.save(diagnosis));
    }

    public void deleteDiagnosis(Long diagnosisId) {
        DiagnosisTemplate diagnosis = diagnosisTemplateRepository.findById(diagnosisId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy chẩn đoán"));
        diagnosisTemplateRepository.delete(diagnosis);
    }

    public List<DiagnosisMedicineRuleResponse> getRulesByDiagnosis(Long diagnosisId) {
        return diagnosisMedicineRuleRepository.findByDiagnosisTemplate_IdOrderByMedicine_MedicineNameAsc(diagnosisId)
                .stream()
                .map(this::toRuleResponse)
                .toList();
    }

    public DiagnosisMedicineRuleResponse createRule(DiagnosisMedicineRuleRequest request) {
        DiagnosisTemplate diagnosis = diagnosisTemplateRepository.findById(request.getDiagnosisId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy chẩn đoán"));
        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy thuốc"));

        validateAgeRange(request.getMinAge(), request.getMaxAge());

        DiagnosisMedicineRule rule = new DiagnosisMedicineRule();
        rule.setDiagnosisTemplate(diagnosis);
        rule.setMedicine(medicine);
        rule.setMinAge(request.getMinAge());
        rule.setMaxAge(request.getMaxAge());
        rule.setDefaultQuantity(request.getDefaultQuantity());
        rule.setDefaultUsage(normalizeOptionalText(request.getDefaultUsage()));

        return toRuleResponse(diagnosisMedicineRuleRepository.save(rule));
    }

    public DiagnosisMedicineRuleResponse updateRule(Long ruleId, DiagnosisMedicineRuleRequest request) {
        DiagnosisMedicineRule rule = diagnosisMedicineRuleRepository.findById(ruleId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy quy tắc thuốc"));

        DiagnosisTemplate diagnosis = diagnosisTemplateRepository.findById(request.getDiagnosisId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy chẩn đoán"));
        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy thuốc"));

        validateAgeRange(request.getMinAge(), request.getMaxAge());

        rule.setDiagnosisTemplate(diagnosis);
        rule.setMedicine(medicine);
        rule.setMinAge(request.getMinAge());
        rule.setMaxAge(request.getMaxAge());
        rule.setDefaultQuantity(request.getDefaultQuantity());
        rule.setDefaultUsage(normalizeOptionalText(request.getDefaultUsage()));

        return toRuleResponse(diagnosisMedicineRuleRepository.save(rule));
    }

    public void deleteRule(Long ruleId) {
        DiagnosisMedicineRule rule = diagnosisMedicineRuleRepository.findById(ruleId)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy quy tắc thuốc"));
        diagnosisMedicineRuleRepository.delete(rule);
    }

    public List<SymptomServiceMappingResponse> getMappingsBySymptom(Long symptomId) {
        return symptomServiceMappingRepository.findBySymptom_Id(symptomId)
                .stream()
                .map(this::toMappingResponse)
                .toList();
    }

    public SymptomServiceMappingResponse createMapping(SymptomServiceMappingRequest request) {
        SymptomTemplate symptom = symptomTemplateRepository.findById(request.getSymptomId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy triệu chứng"));
        MedicalService service = medicalServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy dịch vụ"));

        SymptomServiceMappingId id = new SymptomServiceMappingId();
        id.setSymptomId(symptom.getId());
        id.setServiceId(service.getId());

        if (symptomServiceMappingRepository.existsById(id)) {
            throw AppException.of(HttpStatus.CONFLICT, "Mapping đã tồn tại");
        }

        SymptomServiceMapping mapping = new SymptomServiceMapping();
        mapping.setId(id);
        mapping.setSymptom(symptom);
        mapping.setService(service);

        return toMappingResponse(symptomServiceMappingRepository.save(mapping));
    }

    public void deleteMapping(Long symptomId, Long serviceId) {
        SymptomServiceMappingId id = new SymptomServiceMappingId();
        id.setSymptomId(symptomId);
        id.setServiceId(serviceId);

        SymptomServiceMapping mapping = symptomServiceMappingRepository.findById(id)
                .orElseThrow(() -> AppException.of(HttpStatus.NOT_FOUND, "Không tìm thấy mapping"));
        symptomServiceMappingRepository.delete(mapping);
    }

    private void validateAgeRange(Integer minAge, Integer maxAge) {
        if (minAge == null || maxAge == null) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "Min/max age là bắt buộc");
        }
        if (maxAge < minAge) {
            throw AppException.of(HttpStatus.BAD_REQUEST, "maxAge phải >= minAge");
        }
    }

    private MedicalCategoryResponse toCategoryResponse(MedicalCategory category) {
        return new MedicalCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIsActive());
    }

    private SymptomTemplateResponse toSymptomResponse(SymptomTemplate symptom) {
        MedicalCategory category = symptom.getCategory();
        return new SymptomTemplateResponse(
                symptom.getId(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                symptom.getSymptomName());
    }

    private DiagnosisTemplateResponse toDiagnosisResponse(DiagnosisTemplate diagnosis) {
        MedicalCategory category = diagnosis.getCategory();
        return new DiagnosisTemplateResponse(
                diagnosis.getId(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                diagnosis.getDiagnosisName(),
                diagnosis.getDefaultAdvice());
    }

    private DiagnosisMedicineRuleResponse toRuleResponse(DiagnosisMedicineRule rule) {
        DiagnosisTemplate diagnosis = rule.getDiagnosisTemplate();
        Medicine medicine = rule.getMedicine();
        return new DiagnosisMedicineRuleResponse(
                rule.getId(),
                diagnosis == null ? null : diagnosis.getId(),
                diagnosis == null ? null : diagnosis.getDiagnosisName(),
                medicine == null ? null : medicine.getId(),
                medicine == null ? null : medicine.getMedicineName(),
                rule.getMinAge(),
                rule.getMaxAge(),
                rule.getDefaultQuantity(),
                rule.getDefaultUsage());
    }

    private SymptomServiceMappingResponse toMappingResponse(SymptomServiceMapping mapping) {
        SymptomTemplate symptom = mapping.getSymptom();
        MedicalService service = mapping.getService();
        MedicalCategory category = symptom == null ? null : symptom.getCategory();
        return new SymptomServiceMappingResponse(
                symptom == null ? null : symptom.getId(),
                symptom == null ? null : symptom.getSymptomName(),
                service == null ? null : service.getId(),
                service == null ? null : service.getServiceName(),
                category == null ? null : category.getName());
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw AppException.of(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @SuppressWarnings("unused")
    private String normalizeOptionalTextLower(String value) {
        String normalized = normalizeOptionalText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
