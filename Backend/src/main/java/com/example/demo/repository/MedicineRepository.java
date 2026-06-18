package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Medicine;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    boolean existsByMedicineNameIgnoreCase(String medicineName);

	List<Medicine> findByIsActiveTrueOrderByMedicineNameAsc();

	List<Medicine> findByIsActiveTrueAndMedicineNameContainingIgnoreCaseOrderByMedicineNameAsc(String keyword);
}
