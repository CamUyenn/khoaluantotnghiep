package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.SymptomServiceMapping;
import com.example.demo.entity.SymptomServiceMappingId;

public interface SymptomServiceMappingRepository extends JpaRepository<SymptomServiceMapping, SymptomServiceMappingId> {
    List<SymptomServiceMapping> findBySymptom_IdIn(List<Long> symptomIds);

    List<SymptomServiceMapping> findBySymptom_Id(Long symptomId);
}
