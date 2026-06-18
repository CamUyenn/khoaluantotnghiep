package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.MedicalRecord;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    List<MedicalRecord> findAllByAppointment_IdOrderByCreatedAtDescIdDesc(Long appointmentId);

    boolean existsByAppointment_Id(Long appointmentId);

    List<MedicalRecord> findByAppointment_Patient_Id(Long patientId);

}
