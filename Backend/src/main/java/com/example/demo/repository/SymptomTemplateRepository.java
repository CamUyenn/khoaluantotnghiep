package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.SymptomTemplate;

public interface SymptomTemplateRepository extends JpaRepository<SymptomTemplate, Long> {
    List<SymptomTemplate> findByCategory_IdOrderBySymptomNameAsc(Long categoryId);
}
