package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.MedicalCategory;

public interface MedicalCategoryRepository extends JpaRepository<MedicalCategory, Long> {
    boolean existsByNameIgnoreCase(String name);

    List<MedicalCategory> findByIsActiveTrueOrderByNameAsc();

    List<MedicalCategory> findAllByOrderByNameAsc();
}
