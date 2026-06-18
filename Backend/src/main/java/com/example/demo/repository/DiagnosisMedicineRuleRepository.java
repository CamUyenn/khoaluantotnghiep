package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.DiagnosisMedicineRule;

public interface DiagnosisMedicineRuleRepository extends JpaRepository<DiagnosisMedicineRule, Long> {
    List<DiagnosisMedicineRule> findByDiagnosisTemplate_IdOrderByMedicine_MedicineNameAsc(Long diagnosisId);

    void deleteByDiagnosisTemplate_Id(Long diagnosisId);

    List<DiagnosisMedicineRule> findByDiagnosisTemplate_IdAndMinAgeLessThanEqualAndMaxAgeGreaterThanEqualOrderByMedicine_MedicineNameAsc(
            Long diagnosisId, Integer ageLowerBound, Integer ageUpperBound);
}
