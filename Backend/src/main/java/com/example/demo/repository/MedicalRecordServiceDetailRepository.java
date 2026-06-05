package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.MedicalRecordServiceDetail;
import com.example.demo.entity.MedicalRecordServiceId;

public interface MedicalRecordServiceDetailRepository extends JpaRepository<MedicalRecordServiceDetail, MedicalRecordServiceId> {

    List<MedicalRecordServiceDetail> findByMedicalRecord_Id(Long medicalRecordId);
}
