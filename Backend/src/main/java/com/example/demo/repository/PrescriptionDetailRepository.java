package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.PrescriptionDetail;
import com.example.demo.entity.PrescriptionDetailId;

public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, PrescriptionDetailId> {

    List<PrescriptionDetail> findByMedicalRecord_Id(Long medicalRecordId);

    void deleteByMedicalRecord_Id(Long medicalRecordId);
}
