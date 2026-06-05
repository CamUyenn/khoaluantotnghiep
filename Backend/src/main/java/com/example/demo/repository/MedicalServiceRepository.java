package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.MedicalService;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {

    boolean existsByServiceNameIgnoreCase(String serviceName);

    List<MedicalService> findByIsActiveTrueOrderByServiceNameAsc();

    List<MedicalService> findByIsActiveTrueAndServiceNameContainingIgnoreCaseOrderByServiceNameAsc(String keyword);
}
